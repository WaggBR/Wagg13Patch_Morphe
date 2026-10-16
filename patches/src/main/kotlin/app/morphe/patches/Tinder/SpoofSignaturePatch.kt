package app.morphe.patches.tinder.signature

import app.morphe.patcher.patch.bytecodePatch

private const val HOOK_CLASS = "Lapp/morphe/extension/tinder/patches/SpoofSignaturePatch;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoof the Tinder package signature to fix push notifications " +
        "(Enable to get notifications).",
    default = false,
) {
    compatibleWith("com.tinder")

    extendWith("extensions/extension.mpe")

    execute {
        classDefForEach { classDef ->
            if (classDef.type != HOOK_CLASS && classDef.superclass == "Landroid/app/Application;") {
                mutableClassDefBy(classDef).setSuperClass(HOOK_CLASS)
            }
        }
    }
}
