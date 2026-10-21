package app.morphe.patches.tinder.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.tinder.TinderCompatibility

private const val HOOK_CLASS = "Lapp/morphe/extension/tinder/patches/SpoofSignaturePatch;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoofs the Tinder package signature to fix push notifications " +
        "(Firebase rejects registration when the signature does not match the original).",
    default = true,
) {
    compatibleWith(TinderCompatibility.TINDER)

    extendWith("extensions/extension.mpe")

    execute {
        classDefForEach { classDef ->
            if (classDef.type != HOOK_CLASS && classDef.superclass == "Landroid/app/Application;") {
                mutableClassDefBy(classDef).setSuperClass(HOOK_CLASS)
            }
        }
    }
}
