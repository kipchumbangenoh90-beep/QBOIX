package com.qboix.app;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.text.*;

public class PerformanceAnalyticsActivity extends Activity {
    private final int GOLD = Color.rgb(235, 190, 82);
    private final int BLACK = Color.rgb(7, 10, 16);
    private final int CARD = Color.rgb(19, 25, 36);
    private final int WHITE = Color.rgb(245, 248, 252);
    private final int GREY = Color.rgb(151, 163, 180);
    private final int BLUE = Color.rgb(66, 151, 255);
    private final int GREEN = Color.rgb(55, 211, 145);
    private boolean miles = false;
    private LinearLayout body;
    private final ArrayList<JSONObject> runs = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        JSONArray saved = WorkoutStore.getRunSessions(this);
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.optJSONObject(i);
            if (item != null) runs.add(item);
        }
        Collections.sort(runs, Comparator.comparingLong(o -> o.optLong("savedAt", o.optLong("startedAt", 0))));
        render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(20), dp(15), dp(18), dp(13));
        TextView brand = label("Q-BOIX", GOLD, 23, true);
        brand.setLetterSpacing(0.08f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView tag = label("PERFORMANCE LAB  /  01", GREY, 9, true);
        top.addView(tag);
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(12), dp(18), dp(32));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(label("KNOW YOUR", GREY, 11, true));
        TextView title = label("PERFORMANCE", WHITE, 29, true);
        title.setLetterSpacing(0.025f);
        heading.addView(title);
        heading.addView(label("Your training. Measured. Understood. Improved.", GREY, 11, false));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = dp(17);
        body.addView(heading, hp);

        LinearLayout units = new LinearLayout(this);
        units.setGravity(Gravity.CENTER_VERTICAL);
        units.addView(label("DISTANCE UNITS", GREY, 10, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView toggle = label(miles ? "MILES  ◉" : "KILOMETRES  ◉", BLACK, 10, true);
        toggle.setGravity(Gravity.CENTER);
        toggle.setPadding(dp(14), dp(9), dp(14), dp(9));
        toggle.setBackground(round(GOLD, 22));
        toggle.setOnClickListener(v -> { miles = !miles; render(); });
        units.addView(toggle);
        body.addView(units);
        space(15);

        double total = 0, longest = 0, best = 0;
        int seconds = 0;
        for (JSONObject run : runs) {
            double distance = run.optDouble("distanceMeters", 0);
            double pace = run.optDouble("averagePaceSecondsPerKm", 0);
            total += Math.max(0, distance);
            longest = Math.max(longest, distance);
            seconds += Math.max(0, run.optInt("elapsedSeconds", 0));
            if (pace > 0 && (best == 0 || pace < best)) best = pace;
        }

        section("THE BIG PICTURE", "Your all-time training snapshot");
        statGrid(new String[]{dist(total), time(seconds), pace(best), dist(longest)},
                new String[]{"TOTAL DISTANCE", "TIME ON FEET", "BEST AVG PACE", "LONGEST SESSION"},
                new String[]{"↗", "◷", "⚡", "🏁"},
                new int[]{BLUE, GREEN, GOLD, BLUE});

        section("DISTANCE MOMENTUM", "How far you have travelled in your latest 10 runs");
        ArrayList<Double> distances = new ArrayList<>();
        int start = Math.max(0, runs.size() - 10);
        for (int i = start; i < runs.size(); i++) {
            distances.add(runs.get(i).optDouble("distanceMeters", 0) / (miles ? 1609.344 : 1000));
        }
        chart(distances, BLUE, miles ? "MILES" : "KILOMETRES", false);

        section("PACE EVOLUTION", "Lower pace is faster • compare your recent sessions");
        ArrayList<Double> paces = new ArrayList<>();
        for (int i = start; i < runs.size(); i++) {
            double value = runs.get(i).optDouble("averagePaceSecondsPerKm", 0);
            if (value > 0) paces.add(value * (miles ? 1.609344 : 1));
        }
        chart(paces, GREEN, miles ? "MIN / MILE" : "MIN / KM", true);

        section("TRAINING LOAD", "A quick breakdown of the work you have logged");
        infoRow("GPS sessions", String.valueOf(runs.size()), BLUE);
        infoRow("Total distance", dist(total), GREEN);
        infoRow("Total running time", time(seconds), GOLD);
        infoRow("Longest session", dist(longest), BLUE);
        infoRow("Fartlek sessions", String.valueOf(WorkoutStore.getFartlekSessions(this).length()), GOLD);

        section("YOUR LAST ROUTE", "A trace of your most recently saved GPS session");
        if (runs.isEmpty()) {
            emptyCard("Your route map will appear here after your first saved GPS run.");
        } else {
            JSONObject last = runs.get(runs.size() - 1);
            JSONArray route = last.optJSONArray("route");
            if (route == null || route.length() < 2) {
                emptyCard("This session has no saved route points.");
            } else {
                Route routeView = new Route(route);
                body.addView(routeView, new LinearLayout.LayoutParams(-1, dp(210)));
            }
        }

        if (runs.isEmpty()) {
            space(14);
            emptyCard("YOUR DATA STARTS WITH YOUR NEXT RUN. Complete a GPS session to unlock distance trends, pace analysis and route history.");
        }
        setContentView(root);
    }

    private void statGrid(String[] values, String[] names, String[] symbols, int[] accents) {
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            for (int col = 0; col < 2; col++) {
                int i = rowIndex * 2 + col;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(13), dp(12), dp(14));
                GradientDrawable bg = round(CARD, 17);
                bg.setStroke(dp(1), Color.rgb(39, 49, 66));
                card.setBackground(bg);
                TextView icon = label(symbols[i], accents[i], 17, true);
                card.addView(icon);
                TextView value = label(values[i], WHITE, 21, true);
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
                vp.topMargin = dp(9);
                card.addView(value, vp);
                TextView name = label(names[i], GREY, 9, true);
                name.setLetterSpacing(0.06f);
                LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
                np.topMargin = dp(4);
                card.addView(name, np);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
                cp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(card, cp);
            }
            body.addView(row, new LinearLayout.LayoutParams(-1, dp(112)));
        }
    }

    private void chart(ArrayList<Double> values, int accent, String unit, boolean paceChart) {
        if (values.isEmpty()) {
            emptyCard("Not enough recorded sessions yet. Complete more GPS runs to reveal this trend.");
            return;
        }
        body.addView(new Plot(values, accent, unit, paceChart),
                new LinearLayout.LayoutParams(-1, dp(220)));
        TextView note = label(values.size() + " SESSIONS PLOTTED     •     OLDEST  →  MOST RECENT", GREY, 9, true);
        note.setLetterSpacing(0.035f);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        body.addView(note, np);
    }

    private class Plot extends View {
        final ArrayList<Double> values;
        final int accent;
        final String unit;
        final boolean paceChart;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Plot(ArrayList<Double> v, int c, String u, boolean p) {
            super(PerformanceAnalyticsActivity.this);
            values = v; accent = c; unit = u; paceChart = p;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float left = dp(43), right = w - dp(15), top = dp(31), bottom = h - dp(31);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CARD);
            canvas.drawRoundRect(0, 0, w, h, dp(18), dp(18), paint);

            double min = Collections.min(values), max = Collections.max(values);
            if (Math.abs(max - min) < 0.0001) { min = Math.max(0, min - 1); max += 1; }
            else { double pad = (max - min) * 0.18; min = Math.max(0, min - pad); max += pad; }

            paint.setTextSize(dp(9));
            paint.setColor(GREY);
            canvas.drawText(unit, dp(14), dp(17), paint);
            for (int i = 0; i < 4; i++) {
                float y = top + (bottom - top) * i / 3f;
                paint.setColor(Color.rgb(47, 57, 72));
                paint.setStrokeWidth(dp(1));
                canvas.drawLine(left, y, right, y, paint);
            }

            Path line = new Path();
            Path fill = new Path();
            float[] xs = new float[values.size()];
            float[] ys = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                float x = values.size() == 1 ? (left + right) / 2f : left + (right - left) * i / (values.size() - 1f);
                double ratio = (values.get(i) - min) / (max - min);
                float y = paceChart ? top + (float)ratio * (bottom - top) : bottom - (float)ratio * (bottom - top);
                xs[i] = x; ys[i] = y;
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, bottom); fill.lineTo(x, y); }
                else { line.lineTo(x, y); fill.lineTo(x, y); }
            }
            fill.lineTo(xs[xs.length - 1], bottom);
            fill.close();
            paint.setShader(new LinearGradient(0, top, 0, bottom, Color.argb(105, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)), Shader.TileMode.CLAMP));
            canvas.drawPath(fill, paint);
            paint.setShader(null);
            paint.setColor(accent);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.7f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(line, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < xs.length; i++) {
                paint.setColor(Color.rgb(8, 12, 19));
                canvas.drawCircle(xs[i], ys[i], dp(5), paint);
                paint.setColor(accent);
                canvas.drawCircle(xs[i], ys[i], dp(3), paint);
            }
            paint.setColor(GREY);
            paint.setTextSize(dp(9));
            canvas.drawText("OLD", left, h - dp(11), paint);
            String recent = "RECENT";
            canvas.drawText(recent, right - paint.measureText(recent), h - dp(11), paint);
        }
    }

    private class Route extends View {
        final JSONArray points;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Route(JSONArray a) { super(PerformanceAnalyticsActivity.this); points = a; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight(), pad = dp(22);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CARD);
            canvas.drawRoundRect(0, 0, w, h, dp(18), dp(18), paint);
            double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
            for (int i = 0; i < points.length(); i++) {
                JSONObject p = points.optJSONObject(i);
                if (p == null) continue;
                double lat = p.optDouble("lat"), lon = p.optDouble("lon");
                minLat = Math.min(minLat, lat); maxLat = Math.max(maxLat, lat);
                minLon = Math.min(minLon, lon); maxLon = Math.max(maxLon, lon);
            }
            if (maxLat == minLat || maxLon == minLon) {
                paint.setColor(GREY); paint.setTextSize(dp(11));
                canvas.drawText("Route shape unavailable", pad, h / 2, paint);
                return;
            }
            Path path = new Path();
            for (int i = 0; i < points.length(); i++) {
                JSONObject p = points.optJSONObject(i);
                if (p == null) continue;
                float x = pad + (float)((p.optDouble("lon") - minLon) / (maxLon - minLon)) * (w - 2 * pad);
                float y = h - pad - (float)((p.optDouble("lat") - minLat) / (maxLat - minLat)) * (h - 2 * pad);
                if (path.isEmpty()) path.moveTo(x, y); else path.lineTo(x, y);
            }
            paint.setColor(Color.argb(55, Color.red(BLUE), Color.green(BLUE), Color.blue(BLUE)));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(10)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(path, paint);
            paint.setColor(BLUE); paint.setStrokeWidth(dp(3));
            canvas.drawPath(path, paint);
            JSONObject first = points.optJSONObject(0);
            JSONObject last = points.optJSONObject(points.length() - 1);
            if (first != null && last != null) {
                float sx = pad + (float)((first.optDouble("lon") - minLon) / (maxLon - minLon)) * (w - 2 * pad);
                float sy = h - pad - (float)((first.optDouble("lat") - minLat) / (maxLat - minLat)) * (h - 2 * pad);
                float ex = pad + (float)((last.optDouble("lon") - minLon) / (maxLon - minLon)) * (w - 2 * pad);
                float ey = h - pad - (float)((last.optDouble("lat") - minLat) / (maxLat - minLat)) * (h - 2 * pad);
                paint.setStyle(Paint.Style.FILL); paint.setColor(GREEN); canvas.drawCircle(sx, sy, dp(5), paint);
                paint.setColor(GOLD); canvas.drawCircle(ex, ey, dp(5), paint);
            }
        }
    }

    private void section(String title, String subtitle) {
        TextView heading = label(title, GOLD, 12, true);
        heading.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(22); hp.bottomMargin = dp(4);
        body.addView(heading, hp);
        TextView detail = label(subtitle, GREY, 10, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.bottomMargin = dp(10);
        body.addView(detail, dp);
    }

    private void infoRow(String name, String value, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        row.setBackground(round(CARD, 13));
        TextView left = label(name, GREY, 12, false);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(label(value, accent, 13, true));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = dp(6);
        body.addView(row, rp);
    }

    private void emptyCard(String message) {
        TextView view = label(message, GREY, 11, false);
        view.setPadding(dp(15), dp(17), dp(15), dp(17));
        view.setBackground(round(CARD, 15));
        body.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    private void space(int n) { body.addView(new View(this), new LinearLayout.LayoutParams(1, dp(n))); }
    private TextView label(String value, int color, float size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextColor(color); v.setTextSize(size);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private String dist(double metres) {
        return String.format(Locale.getDefault(), "%.2f %s", metres / (miles ? 1609.344 : 1000), miles ? "mi" : "km");
    }
    private String pace(double secondsPerKm) {
        if (secondsPerKm <= 0) return "--:--";
        int s = (int)Math.round(secondsPerKm * (miles ? 1.609344 : 1));
        return String.format(Locale.getDefault(), "%d:%02d /%s", s / 60, s % 60, miles ? "mi" : "km");
    }
    private String time(int s) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}