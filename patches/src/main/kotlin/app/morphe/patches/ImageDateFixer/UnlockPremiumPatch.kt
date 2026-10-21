package app.morphe.patches.imagedatefixer

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Unlocks premium in Image & Video Date Fixer (eu.duong.imagedatefixer).
 *
 * Ported to 3.10.1 (versionCode 31000010) from the 3.7.1 patch by rushiranpise
 * (https://github.com/rushiranpise/morphe-patches).
 *
 * In 3.10.1 the premium helper class is obfuscated as `nb.c0` (was `gc.c0`).
 * All methods are `public static (Context)Z`:
 * - d() -> prefs "ispremium_lifetime_v5"
 * - e() -> prefs "ispremium_sub"
 * - f() -> prefs "ispremium"
 * - g() -> e() || f() || d()   <- master gate used by every feature check
 *
 * No obfuscated class name is hardcoded: d() is found by its string literal and
 * g() is found inside the same class as the method that calls d().
 *
 * Layer 1: g() always returns true. Layer 2: d() always returns true.
 */
private val IMAGE_DATE_FIXER_COMPATIBILITY = Compatibility(
    name = "Image & Video Date Fixer",
    packageName = "eu.duong.imagedatefixer",
    appIconColor = 0x1565C0,
    apkFileType = ApkFileType.APKM,
    targets = listOf(AppTarget(version = "3.10.1", versionCode = 31000010)),
)

private const val RETURN_TRUE = """
    const/4 v0, 0x1
    return v0
"""

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks all premium features in Image & Video Date Fixer.",
    default = true,
) {
    compatibleWith(IMAGE_DATE_FIXER_COMPATIBILITY)

    execute {
        val premiumClass = IsLifetimeV5Fingerprint.classDef
        val lifetimeMethod = IsLifetimeV5Fingerprint.method

        val isPremiumMethod = premiumClass.methods.firstOrNull { method ->
            method.name != lifetimeMethod.name &&
                method.returnType == "Z" &&
                method.parameterTypes.map { it.toString() } == listOf("Landroid/content/Context;") &&
                (method.accessFlags and AccessFlags.STATIC.value) != 0 &&
                method.implementation?.instructions?.any { instruction ->
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    ref != null &&
                        ref.definingClass == lifetimeMethod.definingClass &&
                        ref.name == lifetimeMethod.name &&
                        ref.returnType == lifetimeMethod.returnType
                } == true
        } ?: throw PatchException(
            "Image Date Fixer: could not find the isPremium gate in ${premiumClass.type}",
        )

        // Layer 1: master gate.
        isPremiumMethod.addInstructions(0, RETURN_TRUE)

        // Layer 2: lifetime flag reader.
        lifetimeMethod.addInstructions(0, RETURN_TRUE)
    }
}
