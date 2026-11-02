package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


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
        pageHeading("MAPS");
        addText("Explore your routes and running locations.", GREY, 14, false);
        addActionCard("GPS ROUTES", "View routes recorded during your runs", GOLD,
                () -> startActivity(new Intent(DashboardActivity.this, PerformanceAnalyticsActivity.class)));
    }

    // ACTIVITIES

    private void showActivities() {

        pageHeading("ACTIVITIES");

        addText("Your running and workout centre.", GREY, 13, false);
        addSpace(8);

        addActivityTile("START GPS RUN", "Record distance, pace, time and route",
                GREEN, "▶", () -> startActivity(new Intent(
                        DashboardActivity.this, GpsRunActivity.class)));

        addActivityTile("PERFORMANCE ANALYTICS", "Charts, pace trends and GPS routes",
                BLUE, "▥", () -> startActivity(new Intent(
                        DashboardActivity.this, PerformanceAnalyticsActivity.class)));

        addActivityTile("WORKOUT HISTORY", "Your completed training sessions",
                GOLD, "◷", () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutHistoryActivity.class)));

        addActivityTile("RUNNING REPORTS", "Create and share Q-BOIX PDF reports",
                Color.rgb(72, 190, 170), "▤", () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutReportsActivity.class)));

        addActivityTile("PERSONAL RECORDS", "Your best performances",
                Color.rgb(165, 125, 235), "★", () -> showPage("Records"));

        addSpace(10);
        TextView videoTitle = text("Q-BOIX ATHLETE SHOWCASE", GOLD, 12, true);
        videoTitle.setLetterSpacing(0.08f);
        mainLayout.addView(videoTitle);

        FrameLayout videoFrame = new FrameLayout(this);
        GradientDrawable videoShape = background(Color.rgb(8, 12, 19), 20);
        videoShape.setStroke(dp(1), Color.argb(150, 215, 181, 91));
        videoFrame.setBackground(videoShape);
        videoFrame.setClipToOutline(true);
        videoFrame.setElevation(dp(5));

        VideoView showcase = new VideoView(this);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                -1, -1, Gravity.CENTER);
        videoFrame.addView(showcase, videoParams);

        TextView videoFallback = text("Loading Q-BOIX video…", GREY, 12, false);
        videoFallback.setGravity(Gravity.CENTER);
        videoFrame.addView(videoFallback, new FrameLayout.LayoutParams(
                -1, -1, Gravity.CENTER));

        showcase.setVideoURI(Uri.parse("android.resource://" +
                getPackageName() + "/" + R.raw.activity_showcase));
        showcase.setOnPreparedListener(player -> {
            videoFallback.setVisibility(View.GONE);
            player.setLooping(true);
            showcase.start();
        });
        showcase.setOnErrorListener((player, what, extra) -> {
            videoFallback.setVisibility(View.VISIBLE);
            videoFallback.setText("Video could not be played. Check the MP4 file.");
            return true;
        });

        LinearLayout.LayoutParams videoCardParams =
                new LinearLayout.LayoutParams(-1, dp(205));
        videoCardParams.topMargin = dp(9);
        videoCardParams.bottomMargin = dp(12);
        mainLayout.addView(videoFrame, videoCardParams);
    }

    private void addActivityTile(String title, String description,
                                 int accent, String symbol, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(13), dp(8), dp(13), dp(8));

        GradientDrawable tileBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.argb(235, 15, 19, 28),
                        Color.argb(218, 24, 27, 35)});
        tileBackground.setCornerRadius(dp(15));
        tileBackground.setStroke(dp(1), Color.argb(105, Color.red(accent), Color.green(accent), Color.blue(accent)));
        tile.setBackground(tileBackground);
        tile.setElevation(dp(3));
        tile.setClickable(true);
        tile.setFocusable(true);

        TextView icon = text(symbol, accent, 23, true);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(11), 0, dp(5), 0);

        TextView heading = text(title, WHITE, 13, true);
        heading.setLetterSpacing(0.025f);
        details.addView(heading);
        TextView sub = text(description, GREY, 10, false);
        LinearLayout.LayoutParams subParams =
                new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(3);
        details.addView(sub, subParams);
        tile.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("›", accent, 25, true);
        arrow.setGravity(Gravity.CENTER);
        tile.addView(arrow, new LinearLayout.LayoutParams(dp(22), -2));

        tile.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(64));
        params.topMargin = dp(6);
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

        addText(
                "Every achievement starts with one step.",
                GREY, 14, false
        );

        addSimpleCard(
                "FIRST RUN",
                "Record your first running activity."
        );

        addSimpleCard(
                "10 KM MILESTONE",
                "Complete 10 kilometres."
        );

        addSimpleCard(
                "CONSISTENCY",
                "Build your training streak."
        );

        addSimpleCard(
                "PERSONAL BEST",
                "Beat one of your previous records."
        );
    }

    // SETTINGS

    private void showSettings() {

        pageHeading("SETTINGS");

        addSimpleCard(
                "ACCOUNT",
                "Account and security settings."
        );

        addSimpleCard(
                "UNITS",
                "Kilometres and miles."
        );

        addSimpleCard(
                "NOTIFICATIONS",
                "Training reminders and alerts."
        );

        addActionCard(
                "WATCH CONNECTION",
                "Connect your GloryFit smartwatch and check live sensor data.",
                GREEN,
                () -> startActivity(new Intent(DashboardActivity.this, WatchActivity.class))
        );

        addSimpleCard(
                "GPS SETTINGS",
                "Location and tracking preferences."
        );

        addSimpleCard(
                "OFFLINE MODE",
                "Local workout storage and synchronization."
        );

        addSimpleCard(
                "PRIVACY",
                "Manage your data and privacy."
        );

        addActionCard(
                "LOG OUT",
                "Sign out of your Q-BOIX account",
                GREY,
                this::confirmLogout
        );
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
