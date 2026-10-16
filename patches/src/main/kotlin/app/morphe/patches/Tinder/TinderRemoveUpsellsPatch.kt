package app.morphe.patches.tinder.upsells

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch


private object SponsoredMessageV17341Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/sponsoredmessage/lifecycle/observer/a;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroidx/lifecycle/f0;"),
)

private object SponsoredMessageV17373Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/sponsoredmessage/lifecycle/observer/a;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Lpuh;"),
)

private object LikesYouUpsellModalV17341Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/likesyoumodal/lifecycle/c;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroidx/lifecycle/f0;"),
)

private object LikesYouUpsellModalV17373Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/likesyoumodal/lifecycle/c;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Lpuh;"),
)


private object BoostUpsellV17341Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/boost/domain/usecase/m;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)

private object BoostUpsellV17373Fingerprint : Fingerprint(
    definingClass = "Lcom/tinder/boost/domain/usecase/e;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
)

private const val RETURN_VOID_SMALI = """
    return-void
"""

private const val RETURN_FALSE_SMALI = """
    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
    return-object v0
"""

@Suppress("unused")
val tinderRemoveUpsellsPatch = bytecodePatch(
    name = "Tinder Remove Upsells",
    description = "Remove sponsored messages in the chat and upsell pop-ups (Likes You, Boost).",
    default = true,
) {
    compatibleWith("com.tinder")

    execute {
        var applied = false

        runCatching {
            SponsoredMessageV17341Fingerprint.method.addInstructions(0, RETURN_VOID_SMALI)
            applied = true
        }
        runCatching {
            SponsoredMessageV17373Fingerprint.method.addInstructions(0, RETURN_VOID_SMALI)
            applied = true
        }

        runCatching {
            LikesYouUpsellModalV17341Fingerprint.method.addInstructions(0, RETURN_VOID_SMALI)
            applied = true
        }
        runCatching {
            LikesYouUpsellModalV17373Fingerprint.method.addInstructions(0, RETURN_VOID_SMALI)
            applied = true
        }

        runCatching {
            BoostUpsellV17341Fingerprint.method.addInstructions(0, RETURN_FALSE_SMALI)
            applied = true
        }
        runCatching {
            BoostUpsellV17373Fingerprint.method.addInstructions(0, RETURN_FALSE_SMALI)
            applied = true
        }

        if (!applied) {
            error("Tinder Remove Upsells: Tinder version not supported. Supported versions: 17.34.1 and 17.37.3.")
        }
    }
}
