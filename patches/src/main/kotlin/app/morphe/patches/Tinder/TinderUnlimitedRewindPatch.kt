package app.morphe.patches.tinder.rewind

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.tinder.TinderCompatibility

// ============================================================================
// VERSÃO SUPORTADA: 17.37.3 (travada via TinderCompatibility)
//
// O R8 renomeia classes, métodos e tipos a cada versão do Tinder, então uma
// fingerprint escrita para esta build não bate com outra. O patch fica preso
// a esta versão de propósito: nomes de uma letra trocam numa atualização, e
// aplicar sem bloqueio fazia o rewind falhar em silêncio noutra versão.
// ============================================================================

// ----------------------------------------------------------------------------
// REWIND — RewindLastRec.invoke() (classe usecase.a nesta versão)
//
// Corpo do método "a":
//   1. Verifica se algum item da lista de swipes é de um tipo "bloqueado".
//   2. Se NÃO for bloqueado -> chama b(swipe), a ação real do rewind.
//   3. Se FOR bloqueado -> fluxo de rewarded-video / paywall.
// O patch troca o corpo inteiro de "a" por: chamar b(swipe) e devolver Unit.
// ----------------------------------------------------------------------------

private object RewindLastRecInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/tinder/library/rewind/internal/usecase/a;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lbax;", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)

// ----------------------------------------------------------------------------
// WATERMARK — construtor da classe que monta o texto de versão (Lee1; nesta versão)
//
// Pega o nome da versão, concatena o build entre parênteses e chama
// Resources.getString(R.string.app_info_version, [texto]). Logo depois do
// getString (instrução 14) o registrador v4 já tem o texto final, localizado.
// O patch concatena o watermark ali (índice 15), então vale pra qualquer idioma.
// ----------------------------------------------------------------------------

private object VersionTextFingerprint : Fingerprint(
    definingClass = "Lee1;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Lfg1;", "Landroid/content/res/Resources;"),
)

private const val WATERMARK_SMALI = """
    const-string v1, " - MOD by @Wagg13 - Morphe"
    invoke-virtual {v4, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
"""

// ============================================================================
// PATCH: Tinder Unlimited Rewind
// (único patch visível — o watermark é aplicado junto, sem entrada própria)
// ============================================================================

@Suppress("unused")
val tinderUnlimitedRewindPatch = bytecodePatch(
    name = "Tinder Unlimited Rewind",
    description = "Makes RewindLastRec.invoke() skip the limit/paywall check and perform the rewind directly.",
    default = true,
) {
    compatibleWith(TinderCompatibility.TINDER)

    execute {
        // --- Rewind ilimitado ---
        RewindLastRecInvokeFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p0, p1}, Lcom/tinder/library/rewind/internal/usecase/a;->b(Lbax;)V
                sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                return-object v0
            """
        )

        // --- Watermark de versão (silencioso: não derruba o patch se falhar) ---
        runCatching {
            VersionTextFingerprint.method.addInstructions(15, WATERMARK_SMALI)
        }
    }
}

/*
 * OBSERVAÇÕES:
 *
 * 1. VERSÃO TRAVADA
 *    Compatibilidade declarada em TinderCompatibility.TINDER (17.37.3). O
 *    Manager recusa aplicar este patch em qualquer outra versão, então a
 *    fingerprint do rewind não precisa mais de fallback/try-catch -- se não
 *    bater, é porque algo mudou DENTRO da 17.37.3 (build diferente do que foi
 *    analisado), e aí o erro do patcher já aponta a causa.
 *
 * 2. SUPORTE A OUTRA VERSÃO NO FUTURO
 *    Achar a classe real via o campo this$0 da continuation
 *    RewindLastRec$invoke$1 (esse nome fica legível), atualizar as
 *    fingerprints acima e adicionar o AppTarget correspondente em
 *    TinderCompatibility.kt.
 *
 * 3. WATERMARK BUNDLADO DE PROPÓSITO
 *    Não há toggle separado nem entrada própria no Morphe. Para desacoplar,
 *    recriar um segundo bytecodePatch só com a fingerprint VersionText.
 *
 * 4. ÍNDICE 15 DO WATERMARK
 *    É fixo (obtido por desmontagem). Se o corpo do construtor mudar, o
 *    watermark pode cair no ponto errado sem erro.
 *
 * 5. CanUserRewind e RecsCardStackViewModel.tryRewind
 *    Continuam sem implementação nesta build (classes vazias após o R8), por
 *    isso o patch não toca neles. Quem chama RewindLastRec é a tela principal
 *    de swipe (MainCardStackRecsViewFragment) e a CuratedCardStackFragment.
 */
