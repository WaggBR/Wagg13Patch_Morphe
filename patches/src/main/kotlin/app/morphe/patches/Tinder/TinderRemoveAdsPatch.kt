package app.morphe.patches.tinder.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.tinder.TinderCompatibility

// ============================================================================
// VERSÃO SUPORTADA: 17.37.3 (travada via TinderCompatibility)
//
// FINGERPRINT 1 — CardStackAdsConfig.<init>(shouldShowAds, isGoogleRecsAdsEnabled, config)
// (classe Lcj4; nesta versão)
//
// Campos confirmados pelo toString() e pelo construtor:
//   a (Z) = shouldShowAds | b (Z) = isGoogleRecsAdsEnabled | c = googleRecsAdConfig
//
// Há um único ponto que constrói o objeto e um único consumidor (FlowCollector
// emit): com shouldShowAds=true ele monta o loader de anúncios (Google direct,
// prioridade HIGH); com false ele para o gerenciador de anúncios. Forçar
// shouldShowAds=false no construtor manda toda emissão pelo caminho "sem anúncios".
// ============================================================================

private object CardStackAdsConfigFingerprint : Fingerprint(
    definingClass = "Lcj4;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Z", "Z", "Le4e;"),
)

// ============================================================================
// FINGERPRINT 2 — ObserveNimbusRecsAdsEnabledImpl (lambda do combine)
//
// Segunda fonte de anúncios do deck (Nimbus): AND de três flags (Z0 && Z1 && Z2).
// O handler de erro do app já emite FALSE, então FALSE é um estado suportado.
// O nome desta classe ficou legível.
// ============================================================================

private object NimbusRecsAdsEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/tinder/adsnimbus/internal/ObserveNimbusRecsAdsEnabledImpl\$invoke\$1;",
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)

// Construtor com 0 locais: p1 == v1 (cabe em const/4). Escrever em p1 antes do
// super-init é válido (só "this" fica não inicializado).
private const val FORCE_SHOULD_SHOW_ADS_FALSE = """
    const/4 p1, 0x0
"""

@Suppress("unused")
val tinderRemoveAdsPatch = bytecodePatch(
    name = "Tinder Remove Ads",
    description = "Removes the ads shown between profiles in the swipe deck.",
    default = true,
) {
    compatibleWith(TinderCompatibility.TINDER)

    execute {
        // --- Anúncios Google no deck: shouldShowAds sempre false ---
        CardStackAdsConfigFingerprint.method.addInstructions(0, FORCE_SHOULD_SHOW_ADS_FALSE)

        // --- Anúncios Nimbus no deck: flow "enabled" sempre false ---
        // invokeSuspend tem 4 locais, então v0 é livre.
        NimbusRecsAdsEnabledFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """
        )
    }
}

/*
 * OBSERVAÇÕES:
 *
 * 1. ESCOPO
 *    Só os anúncios do deck de swipe (Google e Nimbus). Ficam de fora, de
 *    propósito: vídeo recompensado (usado no jogo do Secret Admirer), anúncios
 *    do bouncer paywall e de interesse patrocinado.
 *
 * 2. VERSÃO TRAVADA
 *    "Lcj4;" é nome do R8 e muda a cada versão. Compatibilidade declarada em
 *    TinderCompatibility.TINDER (17.37.3).
 */
