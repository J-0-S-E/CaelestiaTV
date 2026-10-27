package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * The apps in memory as a row of cards, like a tablet's app switcher: OK opens one, Up closes it (frees its memory and
 * takes it off the list), Down reaches Close all, Back returns. Opened by the intent com.gathin.tvhome.RECENTS, e.g.
 * from a Key Mapper double press. Lists the apps Home opened (AppMemory), since Android shows no app the others.
 */
public final class RecentsActivity extends Activity {
    private static final float CARD_W_DP = 192f, CARD_H_DP = 108f;

    private Palette p;
    private float density;
    private LinearLayout row;
    private TextView memory, empty;
    private View closeAll;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Palette scheme = SchemeUi.of(this);
        p = scheme != null ? scheme : Palette.defaults();
        density = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.withAlpha(p.background, 0xF7));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        column.addView(text(getString(R.string.recents_title), 30, p.onSurface, R.string.font_regular));
        memory = text("", 16, p.onSurfaceVariant, R.string.font_regular);
        column.addView(memory, margins(0, 6, 0, 28));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        // The focused card grows past the row: nothing on the way up may cut it or its name.
        scroll.setClipChildren(false);
        column.setClipChildren(false);
        int pad = dp(56);
        scroll.setPadding(pad, dp(20), pad, dp(20));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // Few apps sit centred; more than fit scroll sideways with the focus.
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setClipChildren(false);
        scroll.addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        empty = text(getString(R.string.recents_empty), 18, p.onSurfaceVariant, R.string.font_regular);
        empty.setVisibility(View.GONE);
        column.addView(empty, margins(0, 40, 0, 40));

        TextView close = text(getString(R.string.recents_close_all), 16, p.onSurface, R.string.font_medium);
        close.setFocusable(true);
        close.setPadding(dp(26), dp(10), dp(26), dp(10));
        SchemeUi.pillButton(close, p, density);
        close.setOnClickListener(v -> closeAll());
        closeAll = close;
        column.addView(close, margins(0, 16, 0, 0));

        column.addView(text(getString(R.string.recents_hint), 14, p.onSurfaceVariant, R.string.font_regular),
                margins(0, 22, 0, 0));
        setContentView(root);
        fill();
        updateMemory();
    }

    private void fill() {
        PackageManager pm = getPackageManager();
        for (String pkg : AppMemory.recent(this)) {
            if (pkg.equals(getPackageName()) || launchIntent(pm, pkg) == null) continue;
            row.addView(card(pm, pkg));
        }
        showEmptyIfNone();
        if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private View card(PackageManager pm, String pkg) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setFocusable(true);
        card.setClipChildren(false);

        FrameLayout frame = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(p.surfaceContainerHigh);
        frame.setBackground(bg);
        frame.setClipToOutline(true);
        ImageView art = new ImageView(this);
        Drawable banner = banner(pm, pkg);
        art.setScaleType(banner != null ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        art.setImageDrawable(banner != null ? banner : icon(pm, pkg));
        int inset = banner != null ? 0 : dp(24);
        frame.addView(art, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT) {{ setMargins(inset, inset, inset, inset); }});
        card.addView(frame, new LinearLayout.LayoutParams(dp(CARD_W_DP), dp(CARD_H_DP)));

        TextView name = text(label(pm, pkg), 16, p.onSurfaceVariant, R.string.font_regular);
        card.addView(name, margins(0, 14, 0, 0));
        // Grow from the art's centre, so the name under it stays clear of the larger card.
        card.setPivotY(dp(CARD_H_DP) / 2f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(18), 0, dp(18), 0);
        card.setLayoutParams(lp);

        card.setOnFocusChangeListener((v, focused) -> {
            bg.setStroke(focused ? dp(3) : 0, p.primary);
            name.setTextColor(focused ? p.onSurface : p.onSurfaceVariant);
            v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(120).start();
        });
        card.setOnClickListener(v -> open(pkg));
        card.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) close(v, pkg);
            return true;
        });
        return card;
    }

    private void open(String pkg) {
        Intent i = launchIntent(getPackageManager(), pkg);
        if (i == null) return;
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AppMemory.opened(this, pkg, Config.load(this).backgroundAppLimit);
            finish();
        } catch (RuntimeException ignored) {
            // Gone since the list was made: leave the switcher as it is.
        }
    }

    /** Up: the card flies up and fades, its app is freed, and focus moves to the next card. */
    private void close(View card, String pkg) {
        AppMemory.close(this, pkg);
        int index = row.indexOfChild(card);
        card.setFocusable(false);
        card.animate().translationY(-dp(140)).alpha(0f).setDuration(220).withEndAction(() -> {
            row.removeView(card);
            int n = row.getChildCount();
            if (n > 0) row.getChildAt(Math.min(index, n - 1)).requestFocus();
            showEmptyIfNone();
        }).start();
        handler.postDelayed(this::updateMemory, 700);
    }

    private void closeAll() {
        for (int i = row.getChildCount() - 1; i >= 0; i--) row.getChildAt(i).setFocusable(false);
        for (String pkg : AppMemory.recent(this)) AppMemory.close(this, pkg);
        row.removeAllViews();
        showEmptyIfNone();
        handler.postDelayed(this::updateMemory, 700);
    }

    private void showEmptyIfNone() {
        boolean none = row.getChildCount() == 0;
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        if (none) closeAll.requestFocus();
    }

    /** "Memory in use: 64 %" from /proc/meminfo (MemTotal against MemAvailable). */
    private void updateMemory() {
        long total = -1, available = -1;
        try (BufferedReader in = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = in.readLine()) != null && (total < 0 || available < 0)) {
                if (line.startsWith("MemTotal:")) total = kb(line);
                else if (line.startsWith("MemAvailable:")) available = kb(line);
            }
        } catch (IOException | RuntimeException ignored) {
            // No figure: the line stays empty.
        }
        if (total > 0 && available >= 0) {
            memory.setText(getString(R.string.recents_memory, Math.round(100f * (total - available) / total)));
        }
    }

    private static long kb(String line) {
        return Long.parseLong(line.replaceAll("[^0-9]", ""));
    }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    private static Drawable banner(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationBanner(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static Drawable icon(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static String label(PackageManager pm, String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return Cards.cleanLabel(String.valueOf(pm.getApplicationLabel(ai)), pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private TextView text(String s, float sp, int color, int font) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        try {
            t.setTypeface(UiFont.get(this, font));
        } catch (RuntimeException ignored) {
            // The system font does.
        }
        return t;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        return lp;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
