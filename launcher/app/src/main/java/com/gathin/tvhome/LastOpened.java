package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * When an app was last opened from Home, for the cinematic shelf's "Last opened" line: the launcher notes the
 * time whenever it opens an app. No permission and nothing read from other apps, so an app started some other
 * way (a remote's own Netflix button) shows the last time Home opened it.
 */
final class LastOpened {
    private static final String PREFS = "last_opened";

    private LastOpened() {
    }

    /** Notes that Home just opened this app. */
    static void record(Context ctx, String pkg) {
        prefs(ctx).edit().putLong(pkg, System.currentTimeMillis()).apply();
    }

    /** A use counts half as much after this long, so what the household watches now outranks last month. */
    private static final double HALF_LIFE_MS = 7 * DateUtils.DAY_IN_MILLIS;

    /** Notes that Home just opened this app, for smart_dock: the time, and the use score decayed to now plus one. */
    static void recordUse(Context ctx, String pkg) {
        long now = System.currentTimeMillis();
        prefs(ctx).edit()
                .putLong(pkg, now)
                .putFloat("score:" + pkg, (float) score(ctx, pkg, now) + 1f)
                .apply();
    }

    /** The app's use score at {@code now}: each opening counts 1, halving every week since. 0 when never opened. */
    static double score(Context ctx, String pkg, long now) {
        SharedPreferences p = prefs(ctx);
        float s = p.getFloat("score:" + pkg, 0f);
        if (s <= 0f) return 0;
        long age = Math.max(0L, now - p.getLong(pkg, now));
        return s * Math.pow(0.5, age / HALF_LIFE_MS);
    }

    /** {@code apps} ordered by use score, most used first; apps with the same score keep their listed order. */
    static List<String> rankByUse(Context ctx, List<String> apps) {
        long now = System.currentTimeMillis();
        Map<String, Double> scores = new HashMap<>();
        for (String pkg : apps) scores.put(pkg, score(ctx, pkg, now));
        List<String> ranked = new ArrayList<>(apps);
        // List.sort is stable, so ties (apps never opened) stay in the theme's order.
        ranked.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        return ranked;
    }

    /** Milliseconds since the epoch, or 0 when Home never opened the app. */
    static long get(Context ctx, String pkg) {
        return pkg == null ? 0L : prefs(ctx).getLong(pkg, 0L);
    }

    /**
     * "5 minutes ago" within the hour, then the day and the time: "today, 11:40 am", "yesterday, 9:40 pm",
     * "3 days ago, 9:40 pm", "Oct 12, 9:40 pm". The parts are Android's own wording in the TV's language.
     */
    static String describe(Context ctx, long when) {
        long now = System.currentTimeMillis();
        // Under a minute Android says "0 minutes ago"; one minute reads better.
        when = Math.min(when, now - DateUtils.MINUTE_IN_MILLIS);
        if (now - when < DateUtils.HOUR_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(when, now, DateUtils.MINUTE_IN_MILLIS).toString();
        }
        String day = DateUtils.getRelativeTimeSpanString(when, now, DateUtils.DAY_IN_MILLIS).toString();
        // It follows "Last opened" mid-sentence: "yesterday", not "Yesterday". Month names keep their capital.
        if (Locale.getDefault().getLanguage().equals("en") && (day.startsWith("Yesterday") || day.startsWith("Today"))) {
            day = Character.toLowerCase(day.charAt(0)) + day.substring(1);
        }
        return day + ", " + DateUtils.formatDateTime(ctx, when, DateUtils.FORMAT_SHOW_TIME);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
