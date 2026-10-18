package com.macoshapps.lootvault;

import android.graphics.drawable.GradientDrawable;

/** Visual tokens and drawing helpers shared by all screens. */
public final class Theme {
    public static final int BG = 0xFF0B1220;
    public static final int SURFACE = 0xFF131D2F;
    public static final int SURFACE_HI = 0xFF1C2842;
    public static final int STROKE = 0x1FFFFFFF;
    public static final int ACCENT = 0xFF7AA6FF;
    public static final int ACCENT_ON = 0xFF0B1220;
    public static final int TEXT = 0xFFF2F5FB;
    public static final int MUTED = 0xFF93A2BA;
    public static final int POSITIVE = 0xFF4ADE9C;
    public static final int NEGATIVE = 0xFFFF7D8A;
    public static final int NAV = 0xF2111A2B;

    /** Set once from the Activity before any view is built. */
    public static float density = 1f;

    private Theme() {}

    public static int dp(int value) {
        return Math.round(value * density);
    }

    public static GradientDrawable box(int fill, int radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int from, int to, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {from, to});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }
}
