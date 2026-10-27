package com.gathin.tvhome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The colours the Home screen draws with, as Material 3 roles. The defaults are the white-on-black values the
 * launcher always used, so with no colour scheme set nothing looks different. A scheme is a Caelestia scheme text
 * file ({@code key hexRRGGBB} per line, e.g. {@code primary BD93F9}); unknown keys are ignored and a role the file
 * leaves out keeps its default. Every colour here is opaque ARGB.
 */
final class Palette {
    final int background, surface, surfaceContainer, surfaceContainerHigh;
    final int onSurface, onSurfaceVariant;
    final int primary, onPrimary, secondary, tertiary;
    final int outline, outlineVariant, error;
    /** The background is light, so text on it has to be dark. */
    final boolean isLight;
    /** At least one role came from a scheme file; false for the plain defaults. */
    final boolean custom;

    private static final Palette DEFAULTS = new Palette(new int[Role.values().length], false);

    private enum Role {
        BACKGROUND("background", 0xFF000000),
        SURFACE("surface", 0xFF000000),
        SURFACE_CONTAINER("surfaceContainer", 0xFF141416),
        SURFACE_CONTAINER_HIGH("surfaceContainerHigh", 0xFF1C1C1E),
        ON_SURFACE("onSurface", 0xFFFFFFFF),
        ON_SURFACE_VARIANT("onSurfaceVariant", 0xFFA6A6A6),
        PRIMARY("primary", 0xFFFFFFFF),
        ON_PRIMARY("onPrimary", 0xFF111111),
        SECONDARY("secondary", 0xFF6EC1FF),
        TERTIARY("tertiary", 0xFF34C759),
        OUTLINE("outline", 0xFF8E8E93),
        OUTLINE_VARIANT("outlineVariant", 0xFF3A3A3C),
        ERROR("error", 0xFFFF453A);

        final String key;
        final int fallback;

        Role(String key, int fallback) {
            this.key = key;
            this.fallback = fallback;
        }
    }

    /** values[role ordinal] is the parsed colour, or 0 for a role the file did not set (alpha 0 is never a parsed value). */
    private Palette(int[] values, boolean custom) {
        int[] c = new int[Role.values().length];
        for (Role r : Role.values()) c[r.ordinal()] = values[r.ordinal()] != 0 ? values[r.ordinal()] : r.fallback;
        background = c[Role.BACKGROUND.ordinal()];
        surface = c[Role.SURFACE.ordinal()];
        surfaceContainer = c[Role.SURFACE_CONTAINER.ordinal()];
        surfaceContainerHigh = c[Role.SURFACE_CONTAINER_HIGH.ordinal()];
        // A scheme's own text colours can be faint on its surfaces (Everforest light): nudge them until they read.
        int[] grounds = {c[Role.BACKGROUND.ordinal()], c[Role.SURFACE.ordinal()],
                c[Role.SURFACE_CONTAINER.ordinal()], c[Role.SURFACE_CONTAINER_HIGH.ordinal()]};
        onSurface = readable(c[Role.ON_SURFACE.ordinal()], MIN_CONTRAST, grounds);
        onSurfaceVariant = readable(c[Role.ON_SURFACE_VARIANT.ordinal()], MIN_CONTRAST, grounds);
        primary = c[Role.PRIMARY.ordinal()];
        onPrimary = readable(c[Role.ON_PRIMARY.ordinal()], MIN_CONTRAST, primary);
        secondary = c[Role.SECONDARY.ordinal()];
        tertiary = c[Role.TERTIARY.ordinal()];
        outline = c[Role.OUTLINE.ordinal()];
        outlineVariant = c[Role.OUTLINE_VARIANT.ordinal()];
        error = c[Role.ERROR.ordinal()];
        isLight = luminance(background) > 0.5f;
        this.custom = custom;
    }

    /** WCAG contrast the text roles are held to on the surfaces they are drawn on (AA for body text). */
    static final double MIN_CONTRAST = 4.5;

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** WCAG relative luminance of an RGB colour. */
    static double relativeLuminance(int argb) {
        return 0.2126 * channel((argb >> 16) & 0xFF) + 0.7152 * channel((argb >> 8) & 0xFF) + 0.0722 * channel(argb & 0xFF);
    }

    /** WCAG contrast ratio between two opaque colours, 1 to 21. */
    static double contrast(int a, int b) {
        double la = relativeLuminance(a), lb = relativeLuminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /**
     * {@code fg} itself when it reads at {@code min} on every one of {@code grounds}; otherwise {@code fg} moved
     * towards whichever of black and white reads better on them in steps of 5 percent until it does, or all the way.
     */
    static int readable(int fg, double min, int... grounds) {
        double black = Double.MAX_VALUE, white = Double.MAX_VALUE;   // the worst contrast each end gets on these grounds
        for (int g : grounds) {
            black = Math.min(black, contrast(0xFF000000, g));
            white = Math.min(white, contrast(0xFFFFFFFF, g));
        }
        int target = black >= white ? 0xFF000000 : 0xFFFFFFFF;
        for (int step = 0; step <= 20; step++) {
            int candidate = step == 0 ? fg : mix(fg, target, step * 0.05f);
            boolean ok = true;
            for (int g : grounds) ok &= contrast(candidate, g) >= min;
            if (ok) return candidate;
        }
        return target;
    }

    /** Today's white-on-black colours. */
    static Palette defaults() {
        return DEFAULTS;
    }

    /** Perceived brightness of an RGB colour, 0 (black) to 1 (white). */
    static float luminance(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
    }

    /** The palette in scheme text; defaults when the text is null, empty or has no usable role. Never throws. */
    static Palette parse(String text) {
        if (text == null || text.isEmpty()) return DEFAULTS;
        int[] values = new int[Role.values().length];
        boolean any = false;
        int pos = 0;
        int n = text.length();
        while (pos < n) {
            int end = text.indexOf('\n', pos);
            if (end < 0) end = n;
            String line = text.substring(pos, end).trim();
            pos = end + 1;
            int gap = 0;
            while (gap < line.length() && !Character.isWhitespace(line.charAt(gap))) gap++;
            if (gap == 0 || gap >= line.length()) continue;
            String key = line.substring(0, gap);
            String hex = line.substring(gap + 1).trim();
            Role role = null;
            for (Role r : Role.values()) {
                if (r.key.equals(key)) {
                    role = r;
                    break;
                }
            }
            if (role == null) continue;
            int rgb = parseHex(hex);
            if (rgb < 0) continue;
            values[role.ordinal()] = 0xFF000000 | rgb;
            any = true;
        }
        return any ? new Palette(values, true) : DEFAULTS;
    }

    /** Exactly six hex digits to 0xRRGGBB, or -1. */
    private static int parseHex(String s) {
        if (s.length() != 6) return -1;
        int v = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) return -1;
            v = (v << 4) | d;
        }
        return v;
    }

    /** The scheme file read as UTF-8, or the defaults when it is missing, too large or unreadable. */
    static Palette load(File file) {
        if (file == null || !file.isFile() || file.length() > 64 * 1024) return DEFAULTS;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int len;
            while ((len = in.read(buf)) > 0) sb.append(buf, 0, len);
            return parse(sb.toString());
        } catch (IOException | RuntimeException e) {
            return DEFAULTS;
        }
    }

    /** {@code argb} with its alpha replaced by {@code alpha} (0 to 255). */
    static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** {@code a} moved {@code t} (0 to 1) of the way to {@code b}, opaque. */
    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The scheme file name in a menu: "dracula-dark" becomes "Dracula dark". */
    static String displayName(String fileName) {
        String s = fileName.endsWith(".txt") ? fileName.substring(0, fileName.length() - 4) : fileName;
        s = s.replace('-', ' ').replace('_', ' ').trim();
        return s.isEmpty() ? fileName : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
