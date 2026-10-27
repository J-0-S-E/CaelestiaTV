package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Three small ring gauges for CPU, memory and storage (config stats_style "rings"), drawn on the canvas: a track in
 * outlineVariant, an arc in primary, secondary and tertiary, the percentage in the middle and a short name under each.
 * It redraws only when a value changes, and onDraw allocates nothing.
 */
final class StatsRings extends View {
    private static final int COUNT = 3;
    private static final float RING_DP = 40f, GAP_DP = 14f, CELL_DP = 60f, STROKE_DP = 4f, LABEL_SP = 11f, VALUE_SP = 12f;

    private final float density;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final int[] arcColors = new int[COUNT];
    private final int[] percent = {-1, -1, -1};
    private final String[] percentText = {"-", "-", "-"};
    private final String[] names = new String[COUNT];
    private boolean cpuShown = true;

    StatsRings(Context context) {
        this(context, Palette.defaults());
    }

    StatsRings(Context context, Palette palette) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        names[0] = context.getString(R.string.stats_ring_cpu);
        names[1] = context.getString(R.string.stats_ring_memory);
        names[2] = context.getString(R.string.stats_ring_storage);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(STROKE_DP * density);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(STROKE_DP * density);
        arc.setStrokeCap(Paint.Cap.ROUND);
        value.setTextAlign(Paint.Align.CENTER);
        value.setTextSize(VALUE_SP * density);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(LABEL_SP * density);
        try {
            value.setTypeface(UiFont.get(context, R.string.font_medium));
            label.setTypeface(UiFont.get(context, R.string.font_regular));
        } catch (RuntimeException ignored) {
            // The system font does for a view that only draws numbers and a word.
        }
        setPalette(palette);
    }

    void setPalette(Palette p) {
        track.setColor(p.outlineVariant);
        arcColors[0] = p.primary;
        arcColors[1] = p.secondary;
        arcColors[2] = p.tertiary;
        value.setColor(p.onSurface);
        label.setColor(p.onSurfaceVariant);
        invalidate();
    }

    /** false leaves the CPU ring out, for a TV that gives apps no CPU figure; the other two close up. */
    void setCpuShown(boolean shown) {
        if (shown == cpuShown) return;
        cpuShown = shown;
        requestLayout();
        invalidate();
    }

    /** Each value is a percentage 0 to 100, or -1 when it cannot be read. */
    void setValues(int cpu, int memory, int storage) {
        int[] now = {cpu, memory, storage};
        boolean changed = false;
        for (int i = 0; i < COUNT; i++) {
            int v = now[i] < 0 ? -1 : Math.min(100, now[i]);
            if (v != percent[i]) {
                percent[i] = v;
                percentText[i] = v < 0 ? "-" : Integer.toString(v);
                changed = true;
            }
        }
        if (changed) invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int shown = cpuShown ? COUNT : COUNT - 1;
        int w = Math.round((shown * CELL_DP + (shown - 1) * GAP_DP) * density);
        int h = Math.round((RING_DP + 4 + LABEL_SP + 6) * density);
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float stroke = STROKE_DP * density;
        float ring = RING_DP * density;
        float cell = CELL_DP * density;
        float gap = GAP_DP * density;
        float textShift = (value.descent() + value.ascent()) / 2f;
        int first = cpuShown ? 0 : 1;
        for (int i = first; i < COUNT; i++) {
            float cx = (i - first) * (cell + gap) + cell / 2f;
            float cy = stroke / 2f + ring / 2f;
            float r = (ring - stroke) / 2f;
            oval.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawCircle(cx, cy, r, track);
            if (percent[i] > 0) {
                arc.setColor(arcColors[i]);
                canvas.drawArc(oval, -90f, 360f * percent[i] / 100f, false, arc);
            }
            canvas.drawText(percentText[i], cx, cy - textShift, value);
            canvas.drawText(names[i], cx, ring + stroke + 2f * density - label.ascent(), label);
        }
    }
}
