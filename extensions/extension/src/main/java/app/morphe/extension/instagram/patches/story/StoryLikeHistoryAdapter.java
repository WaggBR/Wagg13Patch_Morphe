package app.morphe.extension.instagram.patches.story;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Two-column grid adapter for the story like history. Each cell is a thumbnail (3:4-ish,
 * rounded top corners, with the story's posted time overlaid) over the author's display name,
 * {@code @username} and the time the story was liked.
 */
@SuppressWarnings("unused")
public final class StoryLikeHistoryAdapter extends BaseAdapter {

    /** Callbacks for a cell's tappable regions, wired by the hosting Activity. */
    public interface Listener {
        void onThumbnail(StoryLikeHistoryEntry entry);

        void onProfile(StoryLikeHistoryEntry entry);

        void onMenu(StoryLikeHistoryEntry entry, View anchor);
    }

    private static final Listener NOOP = new Listener() {
        @Override public void onThumbnail(StoryLikeHistoryEntry entry) { }
        @Override public void onProfile(StoryLikeHistoryEntry entry) { }
        @Override public void onMenu(StoryLikeHistoryEntry entry, View anchor) { }
    };

    private static final int TEXT_PRIMARY = Color.WHITE;
    private static final int TEXT_SECONDARY = 0xFF9AA3AE;
    private static final int ACCENT = 0xFFE1306C;
    private static final int PLACEHOLDER = 0xFF2A2F36;
    private static final int BADGE_BG = 0x99000000;
    private static final int THUMB_HEIGHT_DP = 200;
    private static final int CORNER_DP = 12;

    private final Context context;
    private final Listener listener;
    private final List<StoryLikeHistoryEntry> entries = new ArrayList<>();

    public StoryLikeHistoryAdapter(Context context, List<StoryLikeHistoryEntry> entries) {
        this(context, entries, NOOP);
    }

    public StoryLikeHistoryAdapter(Context context, List<StoryLikeHistoryEntry> entries, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.entries.addAll(entries);
    }

    public void submit(List<StoryLikeHistoryEntry> entries) {
        this.entries.clear();
        this.entries.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public StoryLikeHistoryEntry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StoryLikeHistoryEntry entry = getItem(position);
        View cell = convertView != null ? convertView : buildCell();
        bind((LinearLayout) cell, entry);
        return cell;
    }

    // ---- construction (once per recycled cell) -------------------------------------------

    private LinearLayout buildCell() {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);

        // Thumbnail + posted-time badge + (later) video indicator.
        FrameLayout media = new FrameLayout(context);
        ImageView thumb = new ImageView(context);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable bg = roundedTop();
        bg.setColor(PLACEHOLDER);
        thumb.setBackground(bg);
        thumb.setClipToOutline(true);
        media.addView(thumb, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(THUMB_HEIGHT_DP)));

        TextView badge = new TextView(context);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setBackgroundColor(BADGE_BG);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        badgeLp.setMargins(dp(4), dp(4), 0, 0);
        media.addView(badge, badgeLp);
        cell.addView(media);

        // Text block.
        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(4), dp(8), dp(4), dp(8));

        TextView name = new TextView(context);
        name.setTextColor(TEXT_PRIMARY);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);

        TextView handle = new TextView(context);
        handle.setTextColor(ACCENT);
        handle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        handle.setSingleLine(true);
        handle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(handle);

        TextView liked = new TextView(context);
        liked.setTextColor(TEXT_SECONDARY);
        liked.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        texts.addView(liked);

        cell.addView(texts);
        return cell;
    }

    // ---- binding (every recycle) ---------------------------------------------------------

    private void bind(LinearLayout cell, final StoryLikeHistoryEntry entry) {
        FrameLayout media = (FrameLayout) cell.getChildAt(0);
        ImageView thumb = (ImageView) media.getChildAt(0);
        TextView badge = (TextView) media.getChildAt(1);
        LinearLayout texts = (LinearLayout) cell.getChildAt(1);
        TextView name = (TextView) texts.getChildAt(0);
        TextView handle = (TextView) texts.getChildAt(1);
        TextView liked = (TextView) texts.getChildAt(2);

        applyThumbnail(thumb, entry);

        if (entry.postedAt > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                    .format(new Date(entry.postedAt)));
        } else {
            badge.setVisibility(View.GONE);
        }

        name.setText(entry.displayName == null || entry.displayName.isEmpty() ? entry.username : entry.displayName);
        handle.setText(entry.username == null || entry.username.isEmpty() ? "" : "@" + entry.username);
        liked.setText((pt() ? "Curtido às " : "Liked at ")
                + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(entry.likedAt)));

        thumb.setOnClickListener(v -> listener.onThumbnail(entry));
        name.setOnClickListener(v -> listener.onProfile(entry));
        handle.setOnClickListener(v -> listener.onProfile(entry));
        texts.setOnClickListener(v -> listener.onMenu(entry, texts));
    }

    /** Best-effort thumbnail; placeholder until the local cache is wired up (Task 7). */
    private void applyThumbnail(ImageView thumb, StoryLikeHistoryEntry entry) {
        thumb.setImageDrawable(null);
    }

    private GradientDrawable roundedTop() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{
                dp(CORNER_DP), dp(CORNER_DP), dp(CORNER_DP), dp(CORNER_DP),
                0, 0, 0, 0});
        return drawable;
    }

    private boolean pt() {
        return "pt".equals(Locale.getDefault().getLanguage());
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
