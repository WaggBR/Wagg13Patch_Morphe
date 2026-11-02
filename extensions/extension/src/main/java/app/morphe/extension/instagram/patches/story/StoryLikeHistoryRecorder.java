/*
 * Story like history (Morphe) — capture of the "heart" tap.
 *
 * Instagram's story viewer reports the new like state through
 * LX/0ATY.FhI(LX/04gU; ReelItem; LX/02z4) — the same call the heart button makes. The fields and
 * helper names are the obfuscated ones of the single supported version (452.1.0.53.64), reached by
 * reflection so a rename downgrades this patch to a no-op instead of crashing the app.
 */

package app.morphe.extension.instagram.patches.story;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public final class StoryLikeHistoryRecorder {
    private static final String TAG = "StoryLikeHistory";

    private StoryLikeHistoryRecorder() {
    }

    /**
     * Called from the patched story viewer right before it applies a new like state.
     * Records only {@code LIKED}; anything unexpected is ignored.
     */
    public static void onLikeStateChanged(Object action, Object reelItem) {
        try {
            if (!"LIKED".equals(name(action)) || reelItem == null) return;

            Application app = application();
            if (app == null) return;

            String storyId = text(readField(reelItem, "A1F"));
            if (storyId.isEmpty()) return;

            Object user = readField(reelItem, "A1D");
            String username = callString(user, "A7J");
            String displayName = callString(user, "A7a");
            if (displayName.isEmpty()) displayName = username;

            long postedAt = callLong(reelItem, "A06");
            if (postedAt <= 0L || postedAt == Long.MAX_VALUE) postedAt = 0L;

            StoryLikeHistory.add(app, new StoryLikeHistoryEntry(
                    storyId,
                    "",
                    username,
                    displayName,
                    postedAt,
                    System.currentTimeMillis(),
                    null));

            String thumbnail = imageUrl(reelItem, app);
            if (!thumbnail.isEmpty()) {
                StoryLikeHistory.saveThumbnail(app, storyId, thumbnail, isVideo(reelItem));
            }
        } catch (Throwable t) {
            Log.e(TAG, "could not record story like", t);
        }
    }

    // ---- reflection over the target's obfuscated model --------------------------------

    /** {@code ReelItem.A1F} after a double tap; empty when the field moved. */
    private static Object readField(Object target, String name) {
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    /** {@code User.A7J()}: the username; {@code User.A7a()}: full name. */
    private static String callString(Object target, String method) {
        if (target == null) return "";
        try {
            return text(target.getClass().getMethod(method).invoke(target));
        } catch (Throwable t) {
            return "";
        }
    }

    /** {@code ReelItem.A06()}: the story's publish time, {@link Long#MAX_VALUE} when unset. */
    private static long callLong(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** {@code ReelItem.A0D(Context)}: the cover image the viewer shows for this story. */
    private static String imageUrl(Object reelItem, Context context) {
        try {
            Object imageUrl = reelItem.getClass().getMethod("A0D", Context.class).invoke(reelItem, context);
            if (imageUrl == null) return "";
            Object url = Class.forName("com.instagram.common.typedurl.ImageUrl")
                    .getMethod("getUrl").invoke(imageUrl);
            return text(url);
        } catch (Throwable t) {
            return "";
        }
    }

    /** {@code ReelItem.A0b()} yields a URL only for video stories. */
    private static boolean isVideo(Object reelItem) {
        try {
            Object url = reelItem.getClass().getMethod("A0b").invoke(reelItem);
            return url instanceof String && !((String) url).isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    private static String name(Object enumValue) {
        if (enumValue instanceof Enum) return ((Enum<?>) enumValue).name();
        return "";
    }

    private static Application application() {
        try {
            return (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
