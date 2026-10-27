package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The apps opened from Home, most recent first, for RecentsActivity and background_app_limit. With a limit, opening
 * one more frees the one opened longest ago with ActivityManager.killBackgroundProcesses, which reaches only processes
 * Android already counts as background: an app playing music in a foreground service, or an accessibility helper, is
 * left running. Android does not tell an app which other apps run, so this is the launcher's own list, kept in prefs.
 */
final class AppMemory {
    private static final String PREFS = "app_memory";
    private static final String KEY = "recent";
    /** Remembered at most; older entries drop off the list without being freed. */
    private static final int MAX = 12;

    /** Never freed: the key helpers, casting, the phone remote, the keyboard, speech and Google's own services. */
    private static final Set<String> KEEP = new HashSet<>(Arrays.asList(
            "io.github.sds100.keymapper",
            "com.google.android.apps.mediashell",
            "com.google.android.tv.remote.service",
            "com.google.android.inputmethod.latin",
            "com.google.android.tts",
            "com.google.android.gms",
            "com.android.vending"));

    private AppMemory() {
    }

    /** Home just opened {@code pkg}; frees the apps beyond the {@code limit} most recent. 0 only records it. */
    static void opened(Context ctx, String pkg, int limit) {
        if (pkg == null) return;
        List<String> recent = recent(ctx);
        recent.remove(pkg);
        recent.add(0, pkg);
        if (limit > 0) {
            while (recent.size() > limit) free(ctx, recent.remove(recent.size() - 1));
        }
        while (recent.size() > MAX) recent.remove(recent.size() - 1);
        save(ctx, recent);
    }

    /** The apps Home opened, most recent first. */
    static List<String> recent(Context ctx) {
        String s = prefs(ctx).getString(KEY, "");
        List<String> out = new ArrayList<>();
        for (String pkg : s.split(",")) if (!pkg.isEmpty() && !out.contains(pkg)) out.add(pkg);
        return out;
    }

    /** Frees {@code pkg} and takes it off the list (RecentsActivity's Up key). */
    static void close(Context ctx, String pkg) {
        free(ctx, pkg);
        List<String> recent = recent(ctx);
        recent.remove(pkg);
        save(ctx, recent);
    }

    private static void free(Context ctx, String pkg) {
        if (KEEP.contains(pkg) || pkg.equals(ctx.getPackageName())) return;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;
        try {
            am.killBackgroundProcesses(pkg);
        } catch (RuntimeException ignored) {
            // A firmware that refuses it only keeps the app in memory, as before.
        }
    }

    private static void save(Context ctx, List<String> recent) {
        prefs(ctx).edit().putString(KEY, String.join(",", recent)).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
