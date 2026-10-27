package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * fade_rows: the rows below the dock fade into the bottom edge of the screen instead of being cut off there. Like
 * {@link TopFade} it erases the grid's own picture, so the backdrop shows through; the grid needs its hardware
 * layer (TopFade.attach sets it). Always on, since the rows under the dock always run off the bottom.
 */
final class BottomFade extends RecyclerView.ItemDecoration {

    private final float length;
    private final Paint paint = new Paint();
    private int shaderBottom = -1;

    private BottomFade(float length) {
        this.length = length;
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    static void attach(RecyclerView grid, float length) {
        grid.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        grid.addItemDecoration(new BottomFade(length));
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int bottom = parent.getHeight();
        if (bottom <= 0) return;
        float top = bottom - length;
        if (bottom != shaderBottom) {
            paint.setShader(new LinearGradient(0f, top, 0f, bottom, 0x00000000, 0xFF000000, Shader.TileMode.CLAMP));
            shaderBottom = bottom;
        }
        c.drawRect(0f, top, parent.getWidth(), bottom, paint);
    }
}
