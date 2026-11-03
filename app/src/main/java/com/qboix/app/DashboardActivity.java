package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.provider.Settings;
import android.os.Build;
import android.net.Uri;
import java.io.File;


public class DashboardActivity extends Activity {

    private final int GOLD = Color.rgb(215, 181, 91);
    private final int BLACK = Color.rgb(9, 9, 13);
    private final int CARD = Color.rgb(27, 27, 34);
    private final int WHITE = Color.WHITE;
    private final int GREY = Color.rgb(165, 165, 175);
    private final int BLUE = Color.rgb(52, 136, 255);
    private final int GREEN = Color.rgb(55, 190, 125);

    private LinearLayout mainLayout;
    private LinearLayout navigation;
    private String currentPage = "Home";
    private static final int REQUEST_UPDATE_INSTALL_PERMISSION = 8301;
    private File pendingUpdateApk;
    private final Handler watchRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable watchRefresh = new Runnable(){ @Override public void run(){ refreshWatchMetrics(); watchRefreshHandler.postDelayed(this,1000); } };
    private TextView watchHeartValue, watchOxygenValue, watchStepsValue, watchCaloriesValue, watchConnectionLabel;

    private final String[] tabs = {
            "Home", "Activity", "Training", "Maps", "You"
    };

    private final int[] icons = {
            R.drawable.ic_nav_home, R.drawable.ic_nav_activity,
            R.drawable.ic_nav_training, R.drawable.ic_nav_maps,
            R.drawable.ic_nav_you
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LocalAccountStore.isSignedIn(this)) {
            openLogin();
            return;
        }

        createApplicationLayout();
        showPage("Home");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainLayout != null && "Home".equals(currentPage)) showPage("Home");
        watchRefreshHandler.removeCallbacks(watchRefresh); watchRefreshHandler.post(watchRefresh);
    }

    @Override
    public void onBackPressed() {
        if (!"Home".equals(currentPage)) {
            showPage("Home");
        } else {
            super.onBackPressed();
        }
    }

    private void createApplicationLayout() {

        FrameLayout frame = new FrameLayout(this);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageResource(R.drawable.home_runner_bg);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        View darkWash = new View(this);
        darkWash.setBackgroundColor(Color.argb(105, 0, 0, 0));
        frame.addView(darkWash, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.TRANSPARENT);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.TRANSPARENT);
        mainLayout.setPadding(dp(20), dp(24), dp(20), dp(30));

        scroll.addView(mainLayout);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(4), dp(7), dp(4), dp(7));
        navigation.setBackgroundColor(Color.rgb(17, 17, 23));

        for (int i = 0; i < tabs.length; i++) {

            final String page = tabs[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(7), dp(2), dp(7));

            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(GOLD);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(27), dp(27));
            item.addView(icon, iconParams);

            TextView label = text(tabs[i], GREY, 10, true);
            label.setGravity(Gravity.CENTER);

            item.addView(label);

            item.setOnClickListener(v -> showPage(page));

            navigation.addView(item, new LinearLayout.LayoutParams(
                    0, -2, 1
            ));
        }

        navigation.setBackground(background(Color.argb(220, 7, 8, 13), 24));
        navigation.setElevation(dp(18));
        root.addView(navigation);

        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frame);
    }

    private void showPage(String page) {

        currentPage = page;
        mainLayout.removeAllViews();

        switch (page) {
            case "Home":
                showHome();
                break;

            case "Activity":
                showActivities();
                break;

            case "Training":
                showTraining();
                break;

            case "Maps":
                showMaps();
                break;

            case "You":
                showProfile();
                break;

            case "Analytics":
                startActivity(new Intent(DashboardActivity.this, PerformanceAnalyticsActivity.class));
                break;

            case "Records":
                showRecords();
                break;

            case "Achievements":
                showAchievements();
                break;

            case "Settings":
                showSettings();
                break;
        }

        updateNavigation();
    }

    private void updateNavigation() {
        for (int i = 0; i < navigation.getChildCount(); i++) {
            LinearLayout item = (LinearLayout) navigation.getChildAt(i);
            boolean active = tabs[i].equals(currentPage)
                    || (("Records".equals(currentPage) || "Achievements".equals(currentPage) || "Analytics".equals(currentPage)) && "Activity".equals(tabs[i]))
                    || ("Settings".equals(currentPage) && "You".equals(tabs[i]));
            ImageView icon = (ImageView) item.getChildAt(0);
            TextView label = (TextView) item.getChildAt(1);
            icon.setColorFilter(active ? GREEN : Color.rgb(145, 151, 163));
            label.setTextColor(active ? GREEN : GREY);
            item.setBackground(active ? background(Color.rgb(22, 49, 42), 13) : background(Color.TRANSPARENT, 13));
            item.setContentDescription(tabs[i] + (active ? ", selected" : ""));
        }
    }

    // HOME DASHBOARD

    private void showHome() {
        mainLayout.setPadding(dp(16), dp(12), dp(16), dp(22));
        String name = LocalAccountStore.getName(this);

        // Premium Q-BOIX top bar
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text("Q-BOIX", GOLD, 27, true);
        brand.setLetterSpacing(0.04f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = text("⚙", GOLD, 27, true);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(12), dp(5), dp(12), dp(5));
        settings.setBackground(background(Color.rgb(22, 27, 38), 30));
        settings.setOnClickListener(v -> showPage("Settings"));
        top.addView(settings);
        mainLayout.addView(top);
        addSpace(12);
        // Show live QX11 metrics immediately below the brand header.
        addConnectedWatchBar();

        // Cinematic welcome hero with layered night-to-sunset gradient
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(20), dp(25), dp(16), dp(25));
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5, 16, 38), Color.rgb(20, 48, 91), Color.rgb(190, 100, 37), Color.rgb(8, 12, 22)});
        heroBg.setCornerRadius(dp(24));
        heroBg.setStroke(dp(1), Color.rgb(224, 170, 54));
        hero.setBackground(heroBg);
        TextView welcome = text("Welcome", WHITE, 34, true);
        welcome.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        hero.addView(welcome);
        TextView back = text("Back!", GOLD, 40, true);
        back.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        hero.addView(back);
        TextView greeting = text("Run. Jog. Walk.", Color.rgb(226, 231, 242), 19, false);
        greeting.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1,-2);
        gp.topMargin = dp(8);
        hero.addView(greeting,gp);
        hero.setBackgroundColor(Color.argb(40, 0, 0, 0));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(245));
        hp.bottomMargin = dp(18);
        mainLayout.addView(hero,hp);

        addActivityMode(R.drawable.ic_mode_run, R.drawable.qboix_home_run, "RUN", "Track your run with GPS", () -> startGpsActivity("RUN"));
        addActivityMode(R.drawable.ic_mode_jog, R.drawable.qboix_home_jog, "JOG", "Build rhythm and endurance", () -> startGpsActivity("JOG"));
        addActivityMode(R.drawable.ic_mode_walk, R.drawable.qboix_home_walk, "WALK", "Record a relaxed walk", () -> startGpsActivity("WALK"));
        addSpace(18);
        addText("YOUR PERFORMANCE", GOLD, 12, true);
        addSpace(9);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        addStat(stats, "0.00 km", "DISTANCE");
        addStat(stats, "00:00", "DURATION");
        addView(mainLayout, stats, 0);
        LinearLayout stats2 = new LinearLayout(this);
        stats2.setOrientation(LinearLayout.HORIZONTAL);
        addStat(stats2, "0", "RUNS");
        addStat(stats2, "0 kcal", "CALORIES");
        addView(mainLayout, stats2, 8);
        addSpace(16);
        addText("Welcome, " + name, GREY, 12, false);
    }

    private void addConnectedWatchBar() {
        if (!WatchDataStore.isConnected(this)) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = background(Color.argb(235, 8, 24, 35), 16);
        bg.setStroke(dp(1), Color.rgb(55, 190, 125));
        card.setBackground(bg);
        TextView heading = text("QX11 CONNECTED  •  LIVE WATCH", GREEN, 10, true);
        card.addView(heading);
        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        addWatchMetric(metrics, "♥", "HEART", "heart", "BPM");
        addWatchMetric(metrics, "◉", "SpO₂", "spo2", "%");
        addWatchMetric(metrics, "↗", "STEPS", "steps", "");
        addWatchMetric(metrics, "⚡", "CAL", "calories", "kcal");
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = dp(7);
        card.addView(metrics, mp);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(9);
        mainLayout.addView(card, cp);
    }

    private void addWatchMetric(LinearLayout row, String icon, String title, String key, String unit) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        TextView label = text(title, GREY, 9, true);
        label.setGravity(Gravity.CENTER);
        cell.addView(label);
        int value = WatchDataStore.get(this, key);
        String shown = value < 0 ? "—" : String.valueOf(value);
        TextView number = text(shown, WHITE, 19, true);
        number.setGravity(Gravity.CENTER);
        cell.addView(number);
        if (!unit.isEmpty()) {
            TextView suffix = text(unit, GREEN, 8, false);
            suffix.setGravity(Gravity.CENTER);
            cell.addView(suffix);
        }
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void refreshWatchMetrics() {
        if (watchHeartValue == null) return;
        int heart = WatchDataStore.get(this, "heart");
        int oxygen = WatchDataStore.get(this, "spo2");
        int steps = WatchDataStore.get(this, "steps");
        int calories = WatchDataStore.get(this, "calories");
        watchHeartValue.setText(heart < 0 ? "—" : String.valueOf(heart));
        watchOxygenValue.setText(oxygen < 0 ? "—" : String.valueOf(oxygen));
        watchStepsValue.setText(steps < 0 ? "—" : String.valueOf(steps));
        watchCaloriesValue.setText(calories < 0 ? "—" : String.valueOf(calories));
        boolean watchConnected = WatchDataStore.isConnected(this);
        if (watchConnectionLabel != null) {
            watchConnectionLabel.setText(watchConnected
                    ? "QX11 CONNECTED  •  LIVE WATCH"
                    : "QX11 DISCONNECTED  •  LAST READINGS");
        }
    }

    private void startGpsActivity(String activityType) {
        Intent intent = new Intent(DashboardActivity.this, GpsRunActivity.class);
        intent.putExtra("activity_type", activityType);
        startActivity(intent);
    }

    private void addActivityMode(int iconRes, int sceneRes, String title, String subtitle, Runnable action) {
        FrameLayout card = new FrameLayout(this);

        GradientDrawable cardShape = background(Color.rgb(12, 18, 27), 22);
        cardShape.setStroke(dp(1), Color.rgb(205, 155, 51));
        card.setBackground(cardShape);
        card.setClipToOutline(true);

        ImageView scene = new ImageView(this);
        scene.setImageResource(sceneRes);
        scene.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(scene, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.argb(205, 5, 10, 18), Color.argb(125, 5, 10, 18), Color.argb(35, 5, 10, 18)}));
        card.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(13), dp(9), dp(13), dp(9));

        ImageView figure = new ImageView(this);
        figure.setImageResource(iconRes);
        figure.setColorFilter(Color.rgb(255, 199, 75));
        figure.setScaleType(ImageView.ScaleType.FIT_CENTER);
        figure.setElevation(dp(4));
        content.addView(figure, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(11), 0, dp(4), 0);

        TextView heading = text(title, WHITE, 22, true);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        info.addView(heading);
        info.addView(text(subtitle, Color.rgb(235, 235, 235), 11, false));
        content.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        card.addView(content, new FrameLayout.LayoutParams(-1, -1));
        card.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(91));
        p.bottomMargin = dp(13);
        mainLayout.addView(card, p);
    }

    private void showMaps() {
        pageHeading("MAPS & ROUTES");
        addText("Explore your recorded routes and start a new GPS route.", GREY, 14, false);
        addSpace(10);
        addActionCard("RECORDED GPS ROUTES", "Open your route history, pace charts and activity map.", BLUE,
                () -> startActivity(new Intent(DashboardActivity.this, PerformanceAnalyticsActivity.class)));
        addActionCard("START LIVE ROUTE", "Record a new run and watch your route appear as you move.", GREEN,
                () -> startActivity(new Intent(DashboardActivity.this, GpsRunActivity.class)));
        addActionCard("WORKOUT HISTORY", "Review saved runs and previous routes.", GOLD,
                () -> startActivity(new Intent(DashboardActivity.this, WorkoutHistoryActivity.class)));
    }

    // ACTIVITIES

    private void showActivities() {

        pageHeading("ACTIVITIES");

        addText("Your running and workout centre.", GREY, 13, false);
        addSpace(8);

        addActivityTile("START GPS RUN", "Record distance, pace, time and route",
                GREEN, "🏃‍♂️", () -> startActivity(new Intent(
                        DashboardActivity.this, GpsRunActivity.class)));

        addActivityTile("PERFORMANCE ANALYTICS", "Charts, pace trends and GPS routes",
                BLUE, "📊", () -> startActivity(new Intent(
                        DashboardActivity.this, PerformanceAnalyticsActivity.class)));

        addActivityTile("WORKOUT HISTORY", "Your completed training sessions",
                GOLD, "🕘", () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutHistoryActivity.class)));

        addActivityTile("RUNNING REPORTS", "Create and share Q-BOIX PDF reports",
                Color.rgb(72, 190, 170), "📋", () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutReportsActivity.class)));

        addActivityTile("PERSONAL RECORDS", "Your best performances",
                Color.rgb(165, 125, 235), "🏆", () -> showPage("Records"));

        addSpace(10);

        LinearLayout motivationCard = new LinearLayout(this);
        motivationCard.setOrientation(LinearLayout.VERTICAL);
        motivationCard.setGravity(Gravity.CENTER);
        motivationCard.setPadding(dp(20), dp(19), dp(20), dp(19));
        GradientDrawable motivationBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(10, 31, 48), Color.rgb(12, 57, 48),
                        Color.rgb(20, 25, 38)});
        motivationBackground.setCornerRadius(dp(20));
        motivationBackground.setStroke(dp(1), Color.argb(170, 215, 181, 91));
        motivationCard.setBackground(motivationBackground);
        motivationCard.setElevation(dp(5));

        TextView runnerEmoji = text("🏃‍♂️  ⚡  🏅", GOLD, 27, true);
        runnerEmoji.setGravity(Gravity.CENTER);
        motivationCard.addView(runnerEmoji);

        TextView motivationTitle = text("Q-BOIX MINDSET", GOLD, 11, true);
        motivationTitle.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams motivationTitleParams =
                new LinearLayout.LayoutParams(-2, -2);
        motivationTitleParams.topMargin = dp(12);
        motivationCard.addView(motivationTitle, motivationTitleParams);

        TextView motivationQuote = text(
                "Your pace. Your path. Your progress.\nNever stop moving.",
                WHITE, 19, true);
        motivationQuote.setGravity(Gravity.CENTER);
        motivationQuote.setLineSpacing(dp(4), 1.0f);
        LinearLayout.LayoutParams quoteParams =
                new LinearLayout.LayoutParams(-1, -2);
        quoteParams.topMargin = dp(8);
        motivationCard.addView(motivationQuote, quoteParams);

        TextView motivationFooter = text(
                "DISCIPLINE TODAY • STRONGER TOMORROW", GREY, 9, true);
        motivationFooter.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-2, -2);
        footerParams.topMargin = dp(13);
        motivationCard.addView(motivationFooter, footerParams);

        LinearLayout.LayoutParams motivationParams =
                new LinearLayout.LayoutParams(-1, dp(190));
        motivationParams.topMargin = dp(9);
        motivationParams.bottomMargin = dp(12);
        mainLayout.addView(motivationCard, motivationParams);
    }

    private void addActivityTile(String title, String description,
                                 int accent, String symbol, Runnable action) {
        FrameLayout tile = new FrameLayout(this);
        tile.setClipToOutline(true);
        tile.setElevation(dp(4));

        int sceneRes;
        if (title.contains("GPS RUN")) sceneRes = R.drawable.qboix_home_run;
        else if (title.contains("ANALYTICS")) sceneRes = R.drawable.performance_analytics_bg;
        else if (title.contains("HISTORY")) sceneRes = R.drawable.qboix_home_jog;
        else if (title.contains("REPORTS")) sceneRes = R.drawable.qboix_home_walk;
        else sceneRes = R.drawable.home_runner_bg;

        GradientDrawable tileShape = background(Color.rgb(10, 18, 28), 18);
        tileShape.setStroke(dp(1), Color.argb(155,
                Color.red(accent), Color.green(accent), Color.blue(accent)));
        tile.setBackground(tileShape);

        ImageView scene = new ImageView(this);
        scene.setImageResource(sceneRes);
        scene.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(scene, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.argb(225, 4, 10, 18),
                        Color.argb(175, 5, 13, 22), Color.argb(85, 5, 12, 20)}));
        tile.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(15), dp(9), dp(13), dp(9));

        TextView emoji = text(symbol, WHITE, 29, true);
        emoji.setGravity(Gravity.CENTER);
        GradientDrawable emojiBg = background(
                Color.argb(115, Color.red(accent), Color.green(accent), Color.blue(accent)), 15);
        emojiBg.setStroke(dp(1), Color.argb(170, 255, 255, 255));
        emoji.setBackground(emojiBg);
        content.addView(emoji, new LinearLayout.LayoutParams(dp(53), dp(53)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(13), 0, dp(5), 0);

        TextView heading = text(title, WHITE, 13, true);
        heading.setLetterSpacing(0.035f);
        details.addView(heading);

        TextView sub = text(description, Color.rgb(225, 231, 238), 10, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(4);
        details.addView(sub, subParams);
        content.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("›", GOLD, 27, true);
        arrow.setGravity(Gravity.CENTER);
        content.addView(arrow, new LinearLayout.LayoutParams(dp(24), -2));

        tile.addView(content, new FrameLayout.LayoutParams(-1, -1));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(title + ". " + description);
        tile.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(88));
        params.topMargin = dp(7);
        params.bottomMargin = dp(5);
        mainLayout.addView(tile, params);
    }

    // TRAINING

    private void showTraining() {
        pageHeading("TRAINING CENTRE");
        addText("CHOOSE YOUR SESSION. Q-BOIX WILL TRACK THE JOURNEY.", GREEN, 11, true);
        addSpace(8);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(background(Color.rgb(17, 43, 39), 18));
        hero.addView(text("YOUR NEXT SESSION STARTS HERE", WHITE, 18, true));
        TextView intro = text("Every GPS session opens the live map, distance, pace, time and route controls. The selected discipline sets the session mode.", GREY, 12, false);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2); ip.topMargin = dp(7); hero.addView(intro, ip);
        mainLayout.addView(hero, new LinearLayout.LayoutParams(-1, -2));
        addSpace(10);
        addTrainingTile("FARTLEK", "Speed and recovery intervals with voice cues", "01", GOLD, () -> startActivity(new Intent(DashboardActivity.this, FartlekActivity.class)));
        addTrainingTile("TRACK & FIELD", "Track GPS, laps and split times", "02", BLUE, () -> startGpsActivity("TRACK & FIELD"));
        addTrainingTile("CROSS-COUNTRY", "Trail and hill running GPS session", "03", GREEN, () -> startGpsActivity("CROSS-COUNTRY"));
        addTrainingTile("ROAD RUNNING", "Road distance, pace and route tracking", "04", BLUE, () -> startGpsActivity("ROAD RUNNING"));
        addTrainingTile("MARATHON TRAINING", "Long-distance GPS session and splits", "05", GOLD, () -> startGpsActivity("MARATHON"));
        addTrainingTile("CUSTOM WORKOUT", "Open GPS tracking for your own session", "06", GREY, () -> startGpsActivity("CUSTOM"));
    }

    private void addTrainingTile(String title, String description, String number, int accent, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(14), dp(13), dp(14));
        card.setBackground(background(CARD, 16));
        card.setClickable(true);
        card.setFocusable(true);

        TextView index = text(number, accent, 14, true);
        index.setGravity(Gravity.CENTER);
        index.setBackground(background(Color.rgb(39, 40, 48), 11));
        card.addView(index, new LinearLayout.LayoutParams(dp(43), dp(43)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(13), 0, dp(5), 0);
        details.addView(text(title, WHITE, 14, true));
        TextView sub = text(description, GREY, 11, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        details.addView(sub, subParams);
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("›", accent, 24, true);
        card.addView(arrow);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        mainLayout.addView(card, params);
        card.setOnClickListener(v -> action.run());
    }

    // RECORDS

    private void showRecords() {
        pageHeading("PERSONAL RECORDS");
        addText("YOUR GPS BESTS", GREEN, 12, true);
        addSpace(8);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(background(Color.rgb(20, 48, 42), 18));
        TextView heroTitle = text("EVERY SECOND COUNTS", WHITE, 19, true);
        hero.addView(heroTitle);
        TextView heroInfo = text("Q-BOIX checks your completed GPS runs and keeps the fastest time at each distance.", GREY, 13, false);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(7);
        hero.addView(heroInfo, infoParams);
        addView(mainLayout, hero, 8);

        String[] names = {"800 METRES","1500 METRES","3000 METRES","5000 METRES","10 KILOMETRES","HALF MARATHON","MARATHON"};
        double[] targets = {800,1500,3000,5000,10000,21097.5,42195};
        JSONArray sessions = WorkoutStore.getRunSessions(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        for (int n=0; n<targets.length; n++) {
            int best = Integer.MAX_VALUE;
            long bestDate = 0;
            for (int r=0; r<sessions.length(); r++) {
                JSONObject session = sessions.optJSONObject(r);
                if (session == null) continue;
                String type = session.optString("activityType", session.optString("type","RUN"));
                if ("WALK".equalsIgnoreCase(type)) continue;
                JSONArray samples = session.optJSONArray("distanceTimeSamples");
                if (samples == null) continue;
                JSONObject previousSample = null;
                for (int s=0; s<samples.length(); s++) {
                    JSONObject sample = samples.optJSONObject(s);
                    if (sample == null) continue;
                    double distance = sample.optDouble("distanceMeters",-1);
                    int seconds = sample.optInt("elapsedSeconds",-1);
                    if (previousSample != null) {
                        double previousDistance = previousSample.optDouble("distanceMeters",-1);
                        int previousSeconds = previousSample.optInt("elapsedSeconds",-1);
                        if (previousDistance <= targets[n] && distance >= targets[n] && distance > previousDistance && seconds >= previousSeconds) {
                            double fraction = (targets[n]-previousDistance)/(distance-previousDistance);
                            int checkpoint = (int)Math.round(previousSeconds + fraction*(seconds-previousSeconds));
                            if (checkpoint > 0 && checkpoint < best) {
                                best = checkpoint;
                                bestDate = session.optLong("startedAt",0);
                            }
                            break;
                        }
                    }
                    previousSample = sample;
                }
            }
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16),dp(14),dp(16),dp(14));
            card.setBackground(background(CARD,16));
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView event = text(names[n], WHITE, 14, true);
            top.addView(event,new LinearLayout.LayoutParams(0,-2,1));
            TextView time = text(best==Integer.MAX_VALUE ? "--:--" : formatRecordTime(best), best==Integer.MAX_VALUE?GREY:GREEN, 23, true);
            top.addView(time);
            card.addView(top);
            String detail = best==Integer.MAX_VALUE ? "No qualifying GPS run yet" : "GPS best • " + (bestDate>0?dateFormat.format(new Date(bestDate)):"Date unavailable");
            TextView note = text(detail, GREY, 11, false);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1,-2);
            noteParams.topMargin = dp(5);
            card.addView(note,noteParams);
            addView(mainLayout,card,10);
        }
        addSpace(8);
        addText("GPS checkpoint times are interpolated between recorded location samples. They are training estimates, not certified race results.", GREY, 11, false);
    }

    private String formatRecordTime(int seconds) {
        if (seconds >= 3600) return String.format(Locale.US,"%d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60);
        return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);
    }

    // PROFILE

    private void showProfile() {

        pageHeading("ATHLETE PROFILE");

        String name = LocalAccountStore.getName(this);
        String email = LocalAccountStore.getEmail(this);

        addText(name, WHITE, 25, true);
        addText(email, GREY, 13, false);

        addSpace(20);

        addSimpleCard(
                "ACCOUNT STATUS",
                "Local account on this phone"
        );

        addActionCard(
                "MY ACHIEVEMENTS",
                "View your medals and milestones",
                GOLD,
                () -> showPage("Achievements")
        );

        addActionCard(
                "SETTINGS",
                "Manage account and preferences",
                BLUE,
                () -> showPage("Settings")
        );

        addActionCard(
                "LOG OUT",
                "Sign out of Q-BOIX",
                GREY,
                this::confirmLogout
        );
    }

    // COMMUNITY

    private void showCommunity() {

        pageHeading("Q-BOIX COMMUNITY");

        addText(
                "Connect with athletes and share your journey.",
                GREY, 14, false
        );

        addSimpleCard(
                "ATHLETE NETWORK",
                "Athlete connections will appear here."
        );

        addSimpleCard(
                "RUNNING CLUBS",
                "Discover and join running groups."
        );

        addSimpleCard(
                "CHALLENGES",
                "Compete in athlete challenges."
        );

        addSimpleCard(
                "SHARED ACTIVITIES",
                "Discover activities from other runners."
        );
    }

    // ACHIEVEMENTS

    private void showAchievements() {
        pageHeading("ACHIEVEMENTS");
        addText("Milestones calculated from your saved Q-BOIX workouts.", GREY, 13, false);
        addSpace(10);

        JSONArray runs = WorkoutStore.getRunSessions(this);
        JSONArray fartlek = WorkoutStore.getFartlekSessions(this);
        int runCount = runs.length();
        double totalMetres = 0;
        double longestRun = 0;
        java.util.HashSet<String> activeDays = new java.util.HashSet<>();
        int fastest3000 = Integer.MAX_VALUE;
        for (int i = 0; i < runs.length(); i++) {
            JSONObject item = runs.optJSONObject(i);
            if (item == null) continue;
            double distance = item.optDouble("distanceMeters", 0);
            totalMetres += Math.max(0, distance);
            longestRun = Math.max(longestRun, distance);
            long date = item.optLong("startedAt", 0);
            if (date > 0) activeDays.add(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(date)));
            JSONArray samples = item.optJSONArray("distanceTimeSamples");
            if (samples != null) {
                JSONObject previous = null;
                for (int j = 0; j < samples.length(); j++) {
                    JSONObject sample = samples.optJSONObject(j);
                    if (sample == null) continue;
                    if (previous != null && previous.optDouble("distanceMeters", 0) <= 3000
                            && sample.optDouble("distanceMeters", 0) >= 3000) {
                        int seconds = sample.optInt("elapsedSeconds", Integer.MAX_VALUE);
                        if (seconds > 0) fastest3000 = Math.min(fastest3000, seconds);
                        break;
                    }
                    previous = sample;
                }
            }
        }
        boolean firstRun = runCount > 0;
        boolean tenKm = totalMetres >= 10000;
        boolean consistency = activeDays.size() >= 3;
        boolean personalBest = fastest3000 != Integer.MAX_VALUE;

        addAchievementCard("FIRST RUN", "Record your first GPS running activity.",
                firstRun, runCount > 0 ? runCount + " GPS run(s) recorded" : "Not unlocked yet");
        addAchievementCard("10 KM MILESTONE", "Accumulate 10 kilometres across your GPS runs.",
                tenKm, String.format(Locale.getDefault(), "%.2f km accumulated", totalMetres / 1000.0));
        addAchievementCard("CONSISTENCY", "Train on at least three different days.",
                consistency, activeDays.size() + " active training day(s) recorded");
        addAchievementCard("PERSONAL BEST", "Record a GPS 3 km checkpoint time.",
                personalBest, personalBest ? "3 km checkpoint: " + formatRecordTime(fastest3000)
                        : "A recorded run reaching 3 km is needed");

        addSpace(10);
        addText("TRAINING SUMMARY", GOLD, 13, true);
        addSimpleCard("GPS DISTANCE", String.format(Locale.getDefault(), "%.2f km total • Longest run %.2f km",
                totalMetres / 1000.0, longestRun / 1000.0));
        addSimpleCard("FARTLEK SESSIONS", fartlek.length() + " saved session(s)");
        addText("Achievements are based on workouts saved on this device. Imported or watch-only data is not counted yet.", GREY, 11, false);
    }

    private void addAchievementCard(String title, String description, boolean unlocked, String progress) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(background(CARD, 16));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, unlocked ? GREEN : GOLD, 14, true);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = text(unlocked ? "UNLOCKED ✓" : "IN PROGRESS", unlocked ? GREEN : GREY, 10, true);
        top.addView(status);
        card.addView(top);
        TextView detail = text(description, GREY, 12, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = DashboardActivity.this.dp(7);
        card.addView(detail, dp);
        TextView value = text(progress, unlocked ? GREEN : WHITE, 11, true);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.topMargin = DashboardActivity.this.dp(8);
        card.addView(value, vp);
        addView(mainLayout, card, 9);
    }

    // SETTINGS

    private void showSettings() {

        pageHeading("SETTINGS");

        addActionCard(
                "CONNECTED APPS & HEALTH DATA",
                "Connect Q-BOIX with Health Connect and manage fitness data permissions for compatible apps.",
                BLUE,
                () -> showStyledDialog("CONNECTED APPS & HEALTH DATA",
                        "Health Connect support is not active in this build yet. Q-BOIX continues to store its own workouts and GPS routes on this device.",
                        "OK", null, "CLOSE", null)
        );

        addActionCard("APP CATEGORY",
                "Q-BOIX • Health & Fitness. View app information.",
                GOLD, () -> showSettingDialog("APP CATEGORY",
                        "Q-BOIX is a health and fitness app for running, training, GPS activity and wellness tracking."));

        addActionCard("ACCOUNT", "View the account currently signed in on this phone.",
                BLUE, () -> showSettingDialog("ACCOUNT",
                        "Name: " + LocalAccountStore.getName(this) + "\\nEmail: " + LocalAccountStore.getEmail(this)));

        addActionCard("UNITS", "Choose kilometres or miles for distance displays.",
                GOLD, this::showUnitSettings);

        addActionCard("NOTIFICATIONS", "Choose whether training reminders and alerts are enabled.",
                BLUE, this::showNotificationSettings);

        addActionCard(
                "WATCH CONNECTION",
                "Connect your GloryFit smartwatch and check live sensor data.",
                GREEN,
                () -> startActivity(new Intent(DashboardActivity.this, WatchActivity.class))
        );

        addActionCard("GPS SETTINGS", "Check location permission and GPS tracking guidance.",
                BLUE, this::showGpsSettings);

        addActionCard("OFFLINE MODE", "Your runs and workouts are saved locally on this phone.",
                GREEN, () -> showSettingDialog("OFFLINE MODE",
                        "Q-BOIX stores completed workouts on this phone. Internet is only needed for online features and checking app updates. Your saved local workouts remain available offline."));

        addActionCard("PRIVACY", "Understand what Q-BOIX stores on this phone.",
                GOLD, () -> showSettingDialog("PRIVACY",
                        "Workout history and app preferences are stored locally. Watch readings are only shown when received from the connected watch. You can manage Android permissions in system settings."));

        addActionCard("↻  APP UPDATES",
                "Check GitHub for the latest Q-BOIX version and install it safely.",
                BLUE, this::checkForAppUpdates);

        addActionCard(
                "LOG OUT",
                "Sign out of your Q-BOIX account",
                GREY,
                this::confirmLogout
        );
    }

    private void checkForAppUpdates() {
        Toast.makeText(this, "Checking GitHub for Q-BOIX updates...", Toast.LENGTH_SHORT).show();
        AppUpdateManager.checkForUpdate(this, new AppUpdateManager.CheckCallback() {
            @Override
            public void onUpToDate(String installedVersion) {
                showStyledDialog("Q-BOIX IS UP TO DATE",
                        "Installed version: " + installedVersion,
                        "GREAT", null, "CLOSE", null);
            }

            @Override
            public void onUpdateAvailable(AppUpdateManager.UpdateInfo update) {
                showStyledDialog("Q-BOIX UPDATE AVAILABLE",
                        "Version " + update.versionName + " is ready.\\n\\n"
                                + update.releaseNotes
                                + "\\n\\nYour workouts, routes, login and settings will be kept.",
                        "DOWNLOAD", () -> {
                            Toast.makeText(DashboardActivity.this,
                                    "Downloading and verifying Q-BOIX " + update.versionName + "...",
                                    Toast.LENGTH_LONG).show();
                            AppUpdateManager.downloadUpdate(DashboardActivity.this, update,
                                    new AppUpdateManager.DownloadCallback() {
                                        @Override
                                        public void onDownloaded(File apk) {
                                            pendingUpdateApk = apk;
                                            installPendingUpdate();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            showStyledDialog("UPDATE DOWNLOAD FAILED", message,
                                                    "RETRY", () -> checkForAppUpdates(),
                                                    "CLOSE", null);
                                        }
                                    });
                        }, "LATER", null);
            }

            @Override
            public void onError(String message) {
                showStyledDialog("UPDATE CHECK FAILED",
                        message + "\\n\\nCheck your internet connection and try again.",
                        "RETRY", () -> checkForAppUpdates(), "CLOSE", null);
            }
        });
    }

    private void installPendingUpdate() {
        if (pendingUpdateApk == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            showStyledDialog("ALLOW Q-BOIX UPDATES",
                    "Android needs permission to install an update downloaded by Q-BOIX. Enable 'Allow from this source', then return here. Your verified update file is saved.",
                    "OPEN PERMISSION", () -> {
                        Intent permission = new Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(permission, REQUEST_UPDATE_INSTALL_PERMISSION);
                    }, "CANCEL", null);
            return;
        }
        try {
            AppUpdateManager.installApk(this, pendingUpdateApk);
        } catch (Exception e) {
            showStyledDialog("INSTALLER ERROR",
                    "Android could not open the installer. " + e.getMessage(),
                    "OK", null, "CLOSE", null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UPDATE_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && getPackageManager().canRequestPackageInstalls()) {
                installPendingUpdate();
            } else {
                showStyledDialog("INSTALL PERMISSION NEEDED",
                        "Install permission was not enabled. Your downloaded update is still saved. Tap APP UPDATES again after enabling it.",
                        "OK", null, "CLOSE", null);
            }
        }
    }

    private void showSettingDialog(String title, String message) {
        showStyledDialog(title, message, "GOT IT", null, "CLOSE", null);
    }

    private void showStyledDialog(String title, String message, String positive,
                                  Runnable action, String negative, Runnable cancelAction) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(16));
        GradientDrawable shell = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF171923, 0xFF08090D, 0xFF211B10});
        shell.setCornerRadius(dp(24));
        shell.setStroke(dp(1), 0xFF8A713C);
        panel.setBackground(shell);
        TextView heading = text(title, GOLD, 19, true);
        heading.setGravity(Gravity.CENTER);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(44)));
        View divider = new View(this);
        divider.setBackgroundColor(0x887F6A35);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        TextView body = text(message, WHITE, 14, false);
        body.setLineSpacing(dp(3), 1f);
        body.setPadding(dp(5), dp(16), dp(5), dp(20));
        panel.addView(body);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button cancel = styledDialogButton(negative == null ? "CLOSE" : negative, false);
        Button ok = styledDialogButton(positive, true);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams okp = new LinearLayout.LayoutParams(0, dp(48), 1);
        okp.leftMargin = dp(9);
        buttons.addView(ok, okp);
        panel.addView(buttons);
        cancel.setOnClickListener(v -> { dialog.dismiss(); if (cancelAction != null) cancelAction.run(); });
        ok.setOnClickListener(v -> { dialog.dismiss(); if (action != null) action.run(); });
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * .88), -2);
        }
    }

    private Button styledDialogButton(String caption, boolean gold) {
        Button button = new Button(this);
        button.setText(caption);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTextColor(gold ? Color.BLACK : WHITE);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(gold ? GOLD : 0xFF292B34);
        shape.setCornerRadius(dp(15));
        shape.setStroke(dp(1), gold ? 0xFFD7B55B : 0x667F6A35);
        button.setBackground(shape);
        return button;
    }

    private void showUnitSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("qboix_preferences", MODE_PRIVATE);
        final boolean[] miles = {prefs.getBoolean("use_miles", false)};
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable shell = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF171923, 0xFF08090D, 0xFF211B10});
        shell.setCornerRadius(dp(24));
        shell.setStroke(dp(1), 0xFF8A713C);
        panel.setBackground(shell);
        TextView title = text("DISTANCE UNITS", GOLD, 19, true);
        title.setGravity(Gravity.CENTER);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));
        View divider = new View(this);
        divider.setBackgroundColor(0x887F6A35);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout km = unitChoiceRow("Kilometres (km)", !miles[0]);
        LinearLayout mi = unitChoiceRow("Miles (mi)", miles[0]);
        panel.addView(km, new LinearLayout.LayoutParams(-1, dp(62)));
        panel.addView(mi, new LinearLayout.LayoutParams(-1, dp(62)));
        Runnable refresh = () -> { updateUnitChoice(km, !miles[0]); updateUnitChoice(mi, miles[0]); };
        km.setOnClickListener(v -> { miles[0] = false; refresh.run(); });
        mi.setOnClickListener(v -> { miles[0] = true; refresh.run(); });
        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button cancel = styledDialogButton("CANCEL", false);
        Button save = styledDialogButton("SAVE", true);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams savep = new LinearLayout.LayoutParams(0, dp(48), 1);
        savep.leftMargin = dp(9);
        buttons.addView(save, savep);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(50));
        bp.topMargin = dp(8);
        panel.addView(buttons, bp);
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            prefs.edit().putBoolean("use_miles", miles[0]).apply();
            dialog.dismiss();
            Toast.makeText(this, "Distance units saved", Toast.LENGTH_SHORT).show();
        });
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * .88), -2);
        }
    }

    private LinearLayout unitChoiceRow(String caption, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        TextView label = text(caption, WHITE, 17, false);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView indicator = text(selected ? "✓" : "", Color.BLACK, 17, true);
        indicator.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(selected ? GREEN : Color.TRANSPARENT);
        circle.setStroke(dp(2), selected ? GOLD : GREY);
        indicator.setBackground(circle);
        row.addView(indicator, new LinearLayout.LayoutParams(dp(30), dp(30)));
        return row;
    }

    private void updateUnitChoice(LinearLayout row, boolean selected) {
        TextView indicator = (TextView) row.getChildAt(1);
        indicator.setText(selected ? "✓" : "");
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(selected ? GREEN : Color.TRANSPARENT);
        circle.setStroke(dp(2), selected ? GOLD : GREY);
        indicator.setBackground(circle);
    }

    private void showNotificationSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("qboix_preferences", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("training_notifications", true);
        showStyledDialog("TRAINING NOTIFICATIONS",
                "Training reminders are currently " + (enabled ? "ON" : "OFF") +
                ". Android may also control notifications in system settings.",
                enabled ? "TURN OFF" : "TURN ON", () -> {
                    prefs.edit().putBoolean("training_notifications", !enabled).apply();
                    Toast.makeText(this, "Notification preference saved", Toast.LENGTH_SHORT).show();
                }, "CLOSE", null);
    }

    private void showGpsSettings() {
        boolean fine = android.os.Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED;
        String message = "Precise location permission: " + (fine ? "GRANTED" : "NOT GRANTED") +
                "\n\nGPS recording needs location permission and the phone's Location setting switched on.";
        showStyledDialog("GPS SETTINGS", message,
                fine ? "OPEN LOCATION SETTINGS" : "GRANT LOCATION", () -> {
                    if (!fine && android.os.Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 77);
                    } else {
                        try {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        } catch (Exception e) {
                            showStyledDialog("LOCATION", "Open Android Settings and enable Location.",
                                    "GOT IT", null, "CLOSE", null);
                        }
                    }
                }, "CLOSE", null);
    }

    // REUSABLE UI COMPONENTS

    private void pageHeading(String heading) {

        addText("Q-BOIX", GOLD, 25, true);
        addSpace(22);
        addText(heading, WHITE, 23, true);
        addSpace(8);
    }

    private void addSection(String heading) {

        addSpace(22);
        addText(heading, GOLD, 13, true);
        addSpace(10);
    }

    private void addStat(
            LinearLayout parent,
            String value,
            String label
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(17), dp(13), dp(17));
        card.setBackgroundColor(Color.TRANSPARENT);

        TextView number = text(value, BLUE, 19, true);
        TextView title = text(label, GREEN, 10, true);

        card.addView(number);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);

        titleParams.topMargin = dp(5);
        card.addView(title, titleParams);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, -2, 1);

        params.setMargins(dp(4), dp(4), dp(4), dp(4));

        parent.addView(card, params);
    }

    private void addActionCard(
            String title,
            String description,
            int accent,
            Runnable action
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(background(CARD, 14));
        card.setClickable(true);
        card.setFocusable(true);

        TextView heading = text(title, accent, 15, true);
        TextView detail = text(description, GREY, 12, false);

        card.addView(heading);

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);

        detailParams.topMargin = dp(7);
        card.addView(detail, detailParams);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(11);

        mainLayout.addView(card, params);

        card.setOnClickListener(v -> action.run());
    }

    private void addSimpleCard(
            String title,
            String description
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(background(CARD, 13));

        card.addView(text(title, GOLD, 14, true));

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);

        detailParams.topMargin = dp(7);

        card.addView(
                text(description, GREY, 12, false),
                detailParams
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(10);

        mainLayout.addView(card, params);
    }

    private void addText(
            String value,
            int color,
            float size,
            boolean bold
    ) {

        TextView view = text(value, color, size, bold);
        mainLayout.addView(view);
    }

    private TextView text(
            String value,
            int color,
            float size,
            boolean bold
    ) {

        TextView view = new TextView(this);

        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private void addView(
            LinearLayout parent,
            View child,
            int topMargin
    ) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(topMargin);
        parent.addView(child, params);
    }

    private void addSpace(int size) {

        View space = new View(this);

        mainLayout.addView(
                space,
                new LinearLayout.LayoutParams(1, dp(size))
        );
    }

    private GradientDrawable background(
            int color,
            int radius
    ) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private int dp(float value) {

        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    // ACCOUNT

    private void confirmLogout() {

        new android.app.AlertDialog.Builder(this)
                .setTitle("Log out of Q-BOIX?")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (dialog, which) -> {
                    LocalAccountStore.signOut(this);
                    openLogin();
                })
                .show();
    }

    private void showMessage(String message) {

        new android.app.AlertDialog.Builder(this)
                .setTitle("Q-BOIX")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openLogin() {

        Intent intent = new Intent(
                DashboardActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
