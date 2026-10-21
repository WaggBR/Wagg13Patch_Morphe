package app.morphe.patches.nativecamera.premium

import app.morphe.patcher.Fingerprint

// Anchored on stable SharedPreferences keys / log strings instead of the obfuscated
// CameraViewModel class and method names, which change between APK versions.

// The premium entitlement is read on cold start: a getBoolean("is_premium", false) on the
// "rawcam_prefs" file, stored into the premium StateFlow. Both the prefs file name and the
// key are stable across versions.
object CameraViewModelPremiumInitFingerprint : Fingerprint(
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/app/Application;"),
    strings = listOf("is_premium", "rawcam_prefs"),
)

// The premium setter: writes the "is_premium" flag and (on downgrade) resets premium-only
// features. Forcing its boolean argument true means a later non-premium billing result can
// never downgrade the local entitlement. Anchored on the (Z)V signature + the persisted
// "is_premium" key rather than any obfuscated method name, so it survives renames.
object CameraViewModelSetPremiumFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("is_premium"),
)

// PairIP license check. Present in 1.4 and 1.4.4, absent in 1.4.1 (so this fingerprint is
// optional in the patch). The class belongs to the PairIP library and is not obfuscated, so
// the class/method names are stable. It is invoked from Application.attachBaseContext and
// from LicenseContentProvider.onCreate, so a single early return here covers both entries.
object PairIpCheckLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)
