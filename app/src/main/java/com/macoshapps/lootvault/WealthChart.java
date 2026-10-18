package com.macoshapps.lootvault;

import static com.macoshapps.lootvault.Theme.ACCENT;
import static com.macoshapps.lootvault.Theme.BG;
import static com.macoshapps.lootvault.Theme.MUTED;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Line chart of portfolio value over time, with the peak value and first/last dates labelled. */
public final class WealthChart extends View {

    public static final class Point {
        public final long time;
        public final double value;

        public Point(long time, double value) {
            this.time = time;
            this.value = value;
        }
    }

    private final List<Point> points;
    private final NumberFormat money;
    private final SimpleDateFormat date;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path area = new Path();

    public WealthChart(Context context, List<Point> points, NumberFormat money, SimpleDateFormat date) {
        super(context);
        this.points = points;
        this.money = money;
        this.date = date;
        float d = context.getResources().getDisplayMetrics().density;
        line.setColor(ACCENT);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(3 * d);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        grid.setColor(0x14FFFFFF);
        grid.setStrokeWidth(d);
        label.setColor(MUTED);
        label.setTextSize(11 * d);
        dot.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;
        float d = getResources().getDisplayMetrics().density;
        float left = 12 * d;
        float right = getWidth() - 12 * d;
        float top = 36 * d;
        float bottom = getHeight() - 26 * d;
        int n = points.size();

        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (Point p : points) {
            low = Math.min(low, p.value);
            high = Math.max(high, p.value);
        }
        double peak = high;
        double pad = (high - low) < 1e-9 ? Math.max(1, Math.abs(high) * 0.05) : (high - low) * 0.12;
        low -= pad;
        high += pad;

        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2f;
            canvas.drawLine(left, y, right, y, grid);
        }

        if (n == 1) {
            float y = (float) (bottom - (points.get(0).value - low) / (high - low) * (bottom - top));
            float x = (left + right) / 2f;
            dot.setColor(BG);
            canvas.drawCircle(x, y, 8 * d, dot);
            dot.setColor(ACCENT);
            canvas.drawCircle(x, y, 5.5f * d, dot);
            label.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(money.format(points.get(0).value), left, top - 12 * d, label);
            label.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(date.format(new Date(points.get(0).time)), x, getHeight() - 6 * d, label);
            label.setTextAlign(Paint.Align.LEFT);
            return;
        }

        path.reset();
        area.reset();
        float lastX = right;
        float lastY = bottom;
        for (int i = 0; i < n; i++) {
            float x = left + (right - left) * i / (n - 1f);
            float y = (float) (bottom - (points.get(i).value - low) / (high - low) * (bottom - top));
            if (i == 0) {
                path.moveTo(x, y);
                area.moveTo(x, bottom);
                area.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                area.lineTo(x, y);
            }
            lastX = x;
            lastY = y;
        }
        area.lineTo(lastX, bottom);
        area.close();

        fill.setShader(new LinearGradient(0, top, 0, bottom, 0x557AA6FF, 0x007AA6FF, Shader.TileMode.CLAMP));
        canvas.drawPath(area, fill);
        canvas.drawPath(path, line);

        dot.setColor(BG);
        canvas.drawCircle(lastX, lastY, 7 * d, dot);
        dot.setColor(ACCENT);
        canvas.drawCircle(lastX, lastY, 4.5f * d, dot);

        label.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(money.format(peak), left, top - 12 * d, label);
        canvas.drawText(date.format(new Date(points.get(0).time)), left, getHeight() - 6 * d, label);
        label.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(date.format(new Date(points.get(n - 1).time)), right, getHeight() - 6 * d, label);
        label.setTextAlign(Paint.Align.LEFT);
    }
}
