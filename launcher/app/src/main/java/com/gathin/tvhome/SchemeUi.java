package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The colour scheme applied to the launcher's own panels, menus and settings, which are inflated from XML in their
 * plain white-on-dark look. A layout marks a view with {@code android:tag="scheme:..."}; {@link #skin} then walks the
 * inflated tree once and swaps in drawables and colours built from the {@link Palette}. With no scheme nobody calls
 * it, so the XML values stay exactly as they were. Everything is built once per view, nothing at draw time.
 */
final class SchemeUi {
    private SchemeUi() { }

    /** A glass panel behind a popup or dialog. */
    static final String PANEL = "scheme:panel";
    /** A menu row: transparent idle, primary when focused; its texts follow with onPrimary. */
    static final String ROW = "scheme:row";
    /** Row text, row icon, and the dimmer and mid lines under it. They change colour with the row's focus. */
    static final String TEXT = "scheme:text", ICON = "scheme:icon", DIM = "scheme:dim", MID = "scheme:mid";
    /** Plain texts on a panel: a title, a body and a small heading or hint. */
    static final String TITLE = "scheme:title", BODY = "scheme:body", LABEL = "scheme:label";
    /** A frosted pill button inside a panel (TopButton style). */
    static final String BUTTON = "scheme:button";
    /** A choice row, a heading and a hint on the Launcher settings screen. */
    static final String CHOICE = "scheme:choice", HINT = "scheme:hint";

    /** The scheme's palette when one is active for this theme, otherwise null (plain colours). */
    static Palette of(Context ctx) {
        try {
            Config cfg = Config.load(ctx);
            return cfg.schemeActive() ? cfg.palette : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- pure helpers (unit-tested) --------------------------------------------------------------------------

    /** WCAG contrast ratio between two opaque colours, 1 to 21. */
    static double contrast(int a, int b) {
        return Palette.contrast(a, b);
    }

    /**
     * The white that lifts a rim or a sheen on a dark scheme is invisible on a light one, which needs the darker
     * ink instead: the colour to draw an edge or sheen of the scheme with, before its alpha.
     */
    static int edgeInk(Palette p) {
        return p.isLight ? p.onSurface : 0xFFFFFFFF;
    }

    /** Focused: {@code focused}; otherwise {@code idle}. */
    static ColorStateList focusTint(int idle, int focused) {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{focused, idle});
    }

    // ---- drawables --------------------------------------------------------------------------------------------

    /** glass_panel in the scheme: surfaceContainer fading to surface at about 95 percent, an outlineVariant edge. */
    static GradientDrawable glassPanel(Palette p, float density) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Palette.withAlpha(p.surfaceContainer, 0xF2), Palette.withAlpha(p.surface, 0xF2)});
        g.setCornerRadius(20f * density);
        g.setStroke(Math.max(1, Math.round(density)), p.outlineVariant);
        return g;
    }

    /** The frosted top pill: surfaceContainer, a primary fill when focused, a secondary tint when activated. */
    static StateListDrawable pill(Palette p, float density) {
        float radius = 21f * density;
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(p.primary);
        focused.setCornerRadius(radius);
        GradientDrawable activated = new GradientDrawable();
        activated.setColor(Palette.withAlpha(p.secondary, 0x59));
        activated.setStroke(Math.round(1.5f * density), p.secondary);
        activated.setCornerRadius(radius);
        GradientDrawable idle = new GradientDrawable();
        idle.setColor(Palette.withAlpha(p.surfaceContainer, 0x99));
        idle.setStroke(Math.max(1, Math.round(density)), p.outlineVariant);
        idle.setCornerRadius(radius);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{android.R.attr.state_activated}, activated);
        bg.addState(new int[]{}, idle);
        return bg;
    }

    /** input_row_bg in the scheme: nothing idle, a primary slab when focused. */
    static StateListDrawable row(Palette p, float density) {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(p.primary);
        focused.setCornerRadius(13f * density);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{}, new GradientDrawable());
        return bg;
    }

    /**
     * settings_choice_bg in the scheme: surfaceContainerHigh idle, a primary tint with a primary edge when focused,
     * a secondary tint with a secondary edge when selected.
     */
    static StateListDrawable choice(Palette p, float density) {
        float radius = 14f * density;
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(Palette.withAlpha(p.primary, 0x55));
        focused.setCornerRadius(radius);
        focused.setStroke(Math.round(2 * density), p.primary);
        GradientDrawable selected = new GradientDrawable();
        selected.setColor(Palette.withAlpha(p.secondary, 0x33));
        selected.setCornerRadius(radius);
        selected.setStroke(Math.max(1, Math.round(density)), p.secondary);
        GradientDrawable idle = new GradientDrawable();
        idle.setColor(p.surfaceContainerHigh);
        idle.setCornerRadius(radius);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{android.R.attr.state_selected}, selected);
        bg.addState(new int[]{}, idle);
        return bg;
    }

    // ---- applying ---------------------------------------------------------------------------------------------

    /** Re-colours {@code root} and everything under it that carries a scheme tag. */
    static void skin(View root, Palette p, float density) {
        if (root == null || p == null) return;
        apply(root, p, density);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) skin(group.getChildAt(i), p, density);
        }
    }

    private static void apply(View v, Palette p, float density) {
        if (!(v.getTag() instanceof String)) return;
        String tag = (String) v.getTag();
        if (!tag.startsWith("scheme:")) return;
        TextView text = v instanceof TextView ? (TextView) v : null;
        switch (tag) {
            case PANEL:
                v.setBackground(glassPanel(p, density));
                break;
            case ROW:
                v.setBackground(row(p, density));
                break;
            case TEXT:
                tint(v, focusTint(p.onSurface, p.onPrimary));
                break;
            case ICON:
                tint(v, focusTint(p.onSurface, p.onPrimary));
                break;
            case DIM:
                tint(v, focusTint(p.onSurfaceVariant, Palette.withAlpha(p.onPrimary, 0x99)));
                break;
            case MID:
                tint(v, focusTint(p.onSurfaceVariant, Palette.withAlpha(p.onPrimary, 0xE6)));
                break;
            case TITLE:
                if (text != null) text.setTextColor(p.onSurface);
                break;
            case BODY:
                if (text != null) text.setTextColor(Palette.withAlpha(p.onSurface, 0xD9));
                break;
            case LABEL:
            case HINT:
                if (text != null) text.setTextColor(p.onSurfaceVariant);
                break;
            case BUTTON:
                if (text != null) pillButton(text, p, density);
                break;
            case CHOICE:
                v.setBackground(choice(p, density));
                if (text != null) text.setTextColor(p.onSurface);
                break;
            default:
                break;
        }
    }

    /** A TopButton in the scheme: the frosted pill, onSurface text and icon, onPrimary on the primary fill when focused. */
    static void pillButton(TextView button, Palette p, float density) {
        button.setBackground(pill(p, density));
        tint(button, focusTint(p.onSurface, p.onPrimary));
    }

    private static void tint(View v, ColorStateList colors) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            t.setTextColor(colors);
            t.setCompoundDrawableTintList(colors);
        } else if (v instanceof ImageView) {
            ((ImageView) v).setImageTintList(colors);
        }
    }

    /**
     * The Launcher settings screen: every clickable text is a choice, a text tagged {@link #HINT} a hint, any other
     * text a heading. No tag on the choices, as they get their look from a style.
     */
    static void skinSettings(View root, Palette p, float density) {
        if (root == null || p == null) return;
        if (root instanceof TextView) {
            TextView t = (TextView) root;
            if (t.isClickable()) {
                t.setBackground(choice(p, density));
                t.setTextColor(p.onSurface);
            } else if (SchemeUi.HINT.equals(t.getTag())) {
                t.setTextColor(p.onSurfaceVariant);
            } else {
                t.setTextColor(p.onSurface);
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) skinSettings(group.getChildAt(i), p, density);
        }
    }

    /** A dialog builder whose system theme matches the scheme: the light one for a light scheme, so its text is dark. */
    static AlertDialog.Builder dialog(Context ctx, Palette p) {
        if (p != null && p.isLight) return new AlertDialog.Builder(ctx, android.R.style.Theme_Material_Light_Dialog_Alert);
        return new AlertDialog.Builder(ctx);
    }

    /** Shows the dialog on a rounded surfaceContainerHigh slab when a scheme is active. */
    static void show(AlertDialog.Builder builder, Palette p, float density) {
        AlertDialog dialog = builder.create();
        dialog.show();
        Window w = dialog.getWindow();
        if (p != null && w != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(p.surfaceContainerHigh);
            bg.setCornerRadius(20f * density);
            bg.setStroke(Math.max(1, Math.round(density)), p.outlineVariant);
            w.setBackgroundDrawable(bg);
        }
    }
}
