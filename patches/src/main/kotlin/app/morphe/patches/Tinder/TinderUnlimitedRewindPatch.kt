package app.morphe.patches.tinder.rewind

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch


private object RewindLastRecInvokeV17341Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/library/rewind/internal/usecase/b;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lx02/v0;", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)

private object RewindLastRecInvokeV17373Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/library/rewind/internal/usecase/a;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lbax;", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)


private object VersionTextV17341Fingerprint : Fingerprint(
    definingClass = "Li03/a;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Lpz/a;", "Landroid/content/res/Resources;"),
)

private object VersionTextV17373Fingerprint : Fingerprint(
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


@Suppress("unused")
val tinderUnlimitedRewindPatch = bytecodePatch(
    name = "Tinder Unlimited Rewind",
    description = "Remove signature verification and enable Rewind locally.",
    default = true,
) {
    compatibleWith("com.tinder")

    execute {

        var rewindApplied = false

        runCatching {
            RewindLastRecInvokeV17341Fingerprint.method.addInstructions(
                0,
                """
                    invoke-virtual {p0, p1}, Lcom/tinder/library/rewind/internal/usecase/b;->b(Lx02/v0;)V
                    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                    return-object v0
                """
            )
            rewindApplied = true
        }

        runCatching {
            RewindLastRecInvokeV17373Fingerprint.method.addInstructions(
                0,
                """
                    invoke-virtual {p0, p1}, Lcom/tinder/library/rewind/internal/usecase/a;->b(Lbax;)V
                    sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                    return-object v0
                """
            )
            rewindApplied = true
        }

        if (!rewindApplied) {
            error("Tinder Unlimited Rewind: unsupported Tinder version. Supported versions: 17.34.1 and 17.37.3.")
        }

        runCatching {
            VersionTextV17341Fingerprint.method.addInstructions(15, WATERMARK_SMALI)
        }
        runCatching {
            VersionTextV17373Fingerprint.method.addInstructions(15, WATERMARK_SMALI)
        }
    }
}


