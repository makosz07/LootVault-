package com.macoshapps.lootvault;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import java.util.Locale;

/** Generated cover used when a type or asset has no photo. */
public final class CoverArt extends Drawable {
    private static final int[][] PALETTES = {
        {0xFF4F6BFF, 0xFF1B2A6B},
        {0xFF12B886, 0xFF0B3B33},
        {0xFFF08C3A, 0xFF5C2E12},
        {0xFFB14DD6, 0xFF3A1650}
    };

    private final String letter;
    private final int[] palette;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CoverArt(String name, int seed) {
        String trimmed = name == null ? "" : name.trim();
        letter = trimmed.isEmpty() ? "?" : trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
        palette = PALETTES[Math.abs(seed % PALETTES.length)];
        glyph.setColor(0x66FFFFFF);
        glyph.setTypeface(Typeface.DEFAULT_BOLD);
        glyph.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float w = b.width();
        float h = b.height();
        paint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                palette[0], palette[1], Shader.TileMode.CLAMP));
        canvas.drawRect(b, paint);
        paint.setShader(null);
        paint.setColor(0x1FFFFFFF);
        canvas.drawCircle(b.left + w * 0.82f, b.top + h * 0.18f, Math.min(w, h) * 0.45f, paint);
        canvas.drawCircle(b.left + w * 0.12f, b.top + h * 0.98f, Math.min(w, h) * 0.30f, paint);
        glyph.setTextSize(Math.min(w, h) * 0.42f);
        float baseline = b.top + h / 2f - (glyph.descent() + glyph.ascent()) / 2f;
        canvas.drawText(letter, b.left + w / 2f, baseline, glyph);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter filter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
