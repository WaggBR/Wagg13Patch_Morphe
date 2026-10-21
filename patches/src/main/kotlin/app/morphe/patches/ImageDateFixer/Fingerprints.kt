package app.morphe.patches.imagedatefixer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Premium helper class (obfuscated: `gc.c0` in 3.7.1, `nb.c0` in 3.10.1), method
 * `d(Context)Z` — reads the `ispremium_lifetime_v5` flag from SharedPreferences.
 *
 * Anchored on the string literal + SharedPreferences.getBoolean instead of the
 * obfuscated class/method name, so it survives R8 renaming between versions.
 * `j(Context, Z)V` references the same string but returns void, so the
 * return type keeps this match unique.
 *
 * The patch also uses this fingerprint's `classDef` to locate the master gate
 * `g(Context)Z` in the same class.
 *
 * Verified against 3.10.1 (versionCode 31000010), classes.dex.
 */
object IsLifetimeV5Fingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        string("ispremium_lifetime_v5"),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getBoolean",
        ),
    ),
)
