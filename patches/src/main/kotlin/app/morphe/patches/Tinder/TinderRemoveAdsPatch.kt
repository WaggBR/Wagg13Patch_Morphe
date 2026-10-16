package app.morphe.patches.tinder.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch


private object CardStackAdsConfigV17341Fingerprint : Fingerprint(
    definingClass = "Lf51/d;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Z", "Z", "Lcom/tinder/library/adsrecs/internal/e;"),
)

private object CardStackAdsConfigV17373Fingerprint : Fingerprint(
    definingClass = "Lcj4;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Z", "Z", "Le4e;"),
)

private object NimbusRecsAdsEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/tinder/adsnimbus/internal/ObserveNimbusRecsAdsEnabledImpl\$invoke\$1;",
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)
private const val FORCE_SHOULD_SHOW_ADS_FALSE = """
    const/4 p1, 0x0
"""

@Suppress("unused")
val tinderRemoveAdsPatch = bytecodePatch(
    name = "Tinder Remove Ads",
    description = "Removes ads displayed between profiles in the swipe deck.",
    default = true,
) {
    compatibleWith("com.tinder")

    execute {
        var applied = false

        // --- Anúncios Google no deck: shouldShowAds sempre false ---
        runCatching {
            CardStackAdsConfigV17341Fingerprint.method.addInstructions(0, FORCE_SHOULD_SHOW_ADS_FALSE)
            applied = true
        }
        runCatching {
            CardStackAdsConfigV17373Fingerprint.method.addInstructions(0, FORCE_SHOULD_SHOW_ADS_FALSE)
            applied = true
        }

        runCatching {
            NimbusRecsAdsEnabledFingerprint.method.addInstructions(
                0,
                """
                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v0
                """
            )
            applied = true
        }

        if (!applied) {
            error("Tinder Remove Ads: Tinder version not supported. Supported versions: 17.34.1 and 17.37.3.")
        }
    }
}

