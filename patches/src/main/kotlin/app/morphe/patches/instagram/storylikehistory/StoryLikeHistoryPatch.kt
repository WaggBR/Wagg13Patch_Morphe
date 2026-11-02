/*
 * Story like history for Instagram (Morphe): records the stories the user likes in the last 24h
 * and exposes them through a native screen opened from the story "..." menu.
 *
 * The way a custom entry is added to the story "..." options dialog (append to the label array
 * being built, then intercept the tap) follows Piko's StoryButton
 * <https://github.com/crimera/piko> (GPLv3; see NOTICE.piko).
 */

package app.morphe.patches.instagram.storylikehistory

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.SupportedAbi.ARM64_V8A
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/morphe/extension/instagram/patches/story/StoryLikeHistoryActivity;"
private const val RECORDER_CLASS = "Lapp/morphe/extension/instagram/patches/story/StoryLikeHistoryRecorder;"

/**
 * Builds the `CharSequence[]` shown by the story "..." options dialog (452: LX/0YmS.A0r).
 * It is the only method returning that array that also contains the internal pause entry.
 */
internal object StoryOptionsItemsFingerprint : Fingerprint(
    returnType = "[Ljava/lang/CharSequence;",
    strings = listOf("[INTERNAL] Pause Playback"),
    custom = { method, _ ->
        method.parameterTypes.isNotEmpty() &&
            method.implementation?.instructions?.any { it.opcode == Opcode.RETURN_OBJECT } == true
    },
)

/**
 * Handles a tap on an entry of that dialog (452: LX/0YmS.A0I). Anchored on the same strings the
 * 439 implementation used; the clicked label is its last parameter.
 */
internal object StoryOptionsClickFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("explore_viewer", "friendships/mute_friend_reel/%s/", "[INTERNAL] Pause Playback"),
    custom = { method, _ -> method.parameterTypes.lastOrNull() == "Ljava/lang/CharSequence;" },
)

/**
 * Story viewer's like-state setter (452: LX/0ATY.FhI). The heart button
 * (LX/0hbE.onClick) is its only caller and hands over the state the user asked for, so a single
 * place sees every like — unlike `story_interactions/bulk_story_like/`, which is an internal
 * debug entry of the "..." menu, not the heart.
 */
internal object StoryLikeToggleFingerprint : Fingerprint(
    definingClass = "LX/0ATY;",
    name = "FhI",
    returnType = "V",
    parameters = listOf(
        "LX/04gU;",
        "Lcom/instagram/model/reels/ReelItem;",
        "LX/02z4;",
    ),
)

@Suppress("unused")
val storyLikeHistoryPatch = bytecodePatch(
    name = "Story like history",
    description = "Keeps a local history of the stories you liked in the last 24 hours and shows it " +
        "in a native grid, opened from the story \"...\" menu.",
) {
    compatibleWith(
        Compatibility(
            name = "Instagram",
            packageName = "com.instagram.android",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFC483C,
            targets = listOf(
                AppTarget(
                    version = "452.1.0.53.64",
                    versionCodes = mapOf(ARM64_V8A to 385811006),
                ),
            ),
        ),
    )

    dependsOn(storyLikeHistoryManifestPatch)
    extendWith("extensions/extension.mpe")

    execute {
        // "..." menu: append our label to the array being returned by the builder.
        StoryOptionsItemsFingerprint.method.apply {
            val returnIndex = implementation!!.instructions.indexOfFirst { it.opcode == Opcode.RETURN_OBJECT }
            if (returnIndex < 0) throw PatchException("Story options builder has no return-object")
            val arrayRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                invoke-static {v$arrayRegister}, $EXTENSION_CLASS->addButtons([Ljava/lang/CharSequence;)[Ljava/lang/CharSequence;
                move-result-object v$arrayRegister
                """.trimIndent(),
            )
        }

        // "..." menu click: intercept our label before Instagram's own handling.
        StoryOptionsClickFingerprint.method.apply {
            val classDef = StoryOptionsClickFingerprint.classDef
            val activityField = classDef.fields.firstOrNull { it.type == "Landroid/app/Activity;" }
                ?: throw PatchException("Story options handler has no Activity field")
            val textIndex = parameters.indexOfLast { it.type == "Ljava/lang/CharSequence;" }
            val helperIndex = parameters.indexOfLast { it.type == classDef.type }
            if (textIndex < 0 || helperIndex < 0) throw PatchException("Unexpected story options click signature")

            addInstructionsWithLabels(
                0,
                """
                move-object/from16 v0, p$textIndex
                move-object/from16 v1, p$helperIndex
                iget-object v2, v1, $activityField
                invoke-static {v0, v2}, $EXTENSION_CLASS->buttonAction(Ljava/lang/CharSequence;Landroid/content/Context;)Z
                move-result v0
                if-eqz v0, :wagg13_story_like_history_original
                return-void
                """.trimIndent(),
                ExternalLabel("wagg13_story_like_history_original", getInstruction(0)),
            )
        }

        // Heart tap: record the like before Instagram applies it.
        StoryLikeToggleFingerprint.method.apply {
            addInstructions(
                0,
                """
                invoke-static {p1, p2}, $RECORDER_CLASS->onLikeStateChanged(Ljava/lang/Object;Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
    }
}
