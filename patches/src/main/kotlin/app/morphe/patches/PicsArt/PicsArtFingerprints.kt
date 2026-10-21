package app.template.patches.picsart

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/**
 * Central "ads are disabled" gate (com.picsart.studio.ads.a.g() in 30.9.5).
 *
 * Returns true when the user has the ad-remover entitlement, when ads are
 * force-disabled by config, or when no WebView is available. Every ad entry
 * point (banner, interstitial, native, rewarded preload) bails out when it
 * returns true.
 *
 * Anchored on the two remote-config keys it reads, which do not change between
 * releases (class/method names are obfuscated and DO change).
 *
 * `parameters = listOf()` matters: a synthetic accessor in the native-ad
 * use case reads the same two keys but takes arguments.
 */
internal object AdsDisabledFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("ad_remover_enabled", "ads_force_disabled"),
)

/**
 * toString() of the subscription data class ("ActiveSubscriptionInfo", obfuscated
 * as myobfuscated.iO.a in 30.9.5). Its first constructor argument is
 * `isUserSubscribed`, which feeds ~115 readers: tool/effect locks, crown badges,
 * upgrade prompts, watermark decisions on premium content, etc.
 *
 * We only use this fingerprint to locate the class; the constructor is patched
 * in UnlockPremiumPatch.
 */
internal object ActiveSubscriptionInfoToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("ActiveSubscriptionInfo(isUserSubscribed="),
)

/**
 * MD5 helper of the app-signature class (myobfuscated.NU.a in 30.9.5).
 *
 * Picsart hashes the installed signing certificate (MD5 hex) and compares it with
 * the value stored in the EXIF Make/Model of assets/bg_texture.jpg. On mismatch
 * (any re-signed APK) it waits a random 0-15 s and kills the process
 * (System.exit(15) / Process.killProcess), which looks like "the app goes back
 * to the start screen after a few seconds". There are ~9 call sites, so the
 * patch spoofs the hash getter instead of removing each check.
 *
 * Anchored on the unique "AppSignature" log tag + "MD5"; only used to locate the class.
 */
internal object AppSignatureHashHelperFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf("[B"),
    strings = listOf("AppSignature", "MD5"),
)

/**
 * ResolveCorrectSubsBadgeUseCase.shouldShowBadge(tab) — the use case behind the
 * "Try PRO" pill in the home toolbar (badge_home) and the create tab.
 *
 * Obfuscation renames the outer class (com.picsart.subscription.trypro.domain.a in
 * 30.9.5) and the method, but the Kotlin coroutine continuation keeps its original
 * name, so we anchor on the `instance-of` check against that continuation type.
 */
internal object ShouldShowSubsBadgeFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lkotlin/coroutines/jvm/internal/ContinuationImpl;",
    ),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference
            (reference as? TypeReference)?.type ==
                "Lcom/picsart/subscription/trypro/domain/ResolveCorrectSubsBadgeUseCase\$shouldShowBadge\$1;"
        } == true
    },
)
