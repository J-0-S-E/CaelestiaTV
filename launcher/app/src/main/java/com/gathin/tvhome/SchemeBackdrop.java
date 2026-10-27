package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * The scheme's own background (config scheme_backdrop): the background colour with a few soft glows, drawn into one
 * screen-sized bitmap and reused until the scheme or the screen size changes. Nothing is drawn per frame but the bitmap.
 */
final class SchemeBackdrop extends Drawable {
    private static Bitmap cached;
    private static long cachedKey;

    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    SchemeBackdrop(Palette palette, int width, int height) {
        bitmap = bitmapFor(palette, Math.max(1, width), Math.max(1, height));
    }

    private static synchronized Bitmap bitmapFor(Palette p, int w, int h) {
        long key = ((((long) p.background * 31 + p.primary) * 31 + p.tertiary) * 31 + p.secondary) * 31 + w * 7919L + h;
        if (cached != null && !cached.isRecycled() && cachedKey == key && cached.getWidth() == w && cached.getHeight() == h) {
            return cached;
        }
        cached = render(p, w, h);
        cachedKey = key;
        return cached;
    }

    /**
     * An aurora of the scheme's accents over its background: wide soft ellipses in primary, tertiary and secondary,
     * a blend of the two main accents top left, then a darker floor so the tiles stay easy to read. Calmer on a
     * light scheme.
     */
    static Bitmap render(Palette p, int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        int deep = Palette.mix(p.background, p.isLight ? 0xFFFFFFFF : 0xFF000000, 0.35f);
        paint.setShader(new LinearGradient(0, 0, 0, h, p.background, deep, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        float big = Math.max(w, h);
        float k = p.isLight ? 0.55f : 1f;
        glow(c, paint, w * 0.82f, h * 0.02f, big * 0.62f, 1.7f, p.primary, Math.round(0x78 * k));
        glow(c, paint, w * 0.10f, h * 0.92f, big * 0.50f, 1.5f, p.tertiary, Math.round(0x58 * k));
        glow(c, paint, w * 0.62f, h * 0.62f, big * 0.38f, 1.9f, p.secondary, Math.round(0x26 * k));
        glow(c, paint, w * 0.22f, h * 0.10f, big * 0.32f, 1.4f, Palette.mix(p.primary, p.tertiary, 0.5f), Math.round(0x30 * k));
        // The floor: the lower third darkens back toward the background, under the dock and the grid.
        paint.setShader(new LinearGradient(0, h * 0.55f, 0, h, Palette.withAlpha(deep, 0), Palette.withAlpha(deep, 0xB0),
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
        return bmp;
    }

    /** A soft glow centred on (cx, cy), stretched {@code stretch} times wider than tall. */
    private static void glow(Canvas c, Paint paint, float cx, float cy, float radius, float stretch, int rgb, int alpha) {
        int core = Palette.withAlpha(rgb, alpha);
        int clear = Palette.withAlpha(rgb, 0);
        RadialGradient g = new RadialGradient(0, 0, radius, new int[]{core, Palette.withAlpha(rgb, alpha / 3), clear},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        m.setScale(stretch, 1f);
        m.postTranslate(cx, cy);
        g.setLocalMatrix(m);
        paint.setShader(g);
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
        paint.setShader(null);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        android.graphics.Rect b = getBounds();
        if (b.width() == bitmap.getWidth() && b.height() == bitmap.getHeight()) {
            canvas.drawBitmap(bitmap, b.left, b.top, null);
        } else {
            canvas.drawBitmap(bitmap, null, b, paint);
        }
    }

    @Override public void setAlpha(int alpha) { }

    @Override public void setColorFilter(ColorFilter colorFilter) { }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
