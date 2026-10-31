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

            LinearLayout item =
                    (LinearLayout) navigation.getChildAt(i);

            boolean selected = tabs[i].equals(currentPage);

            ImageView icon = (ImageView) item.getChildAt(0);
            TextView label = (TextView) item.getChildAt(1);

            icon.setColorFilter(selected ? Color.rgb(255, 211, 104) : Color.rgb(181, 145, 67));
            label.setTextColor(selected ? Color.rgb(255, 218, 130) : GREY);

            item.setBackground(selected
                    ? background(Color.rgb(38, 34, 25), 10)
                    : background(Color.TRANSPARENT, 10));
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

        addActivityMode(R.drawable.ic_mode_run, "RUN", "Track your run with GPS", new int[]{Color.rgb(10,20,37),Color.rgb(28,58,91)}, () -> startActivity(new Intent(DashboardActivity.this, GpsRunActivity.class)));
        addActivityMode(R.drawable.ic_mode_jog, "JOG", "Build rhythm and endurance", new int[]{Color.rgb(25,30,42),Color.rgb(48,58,75)}, () -> startActivity(new Intent(DashboardActivity.this, GpsRunActivity.class)));
        addActivityMode(R.drawable.ic_mode_walk, "WALK", "Record a relaxed walk", new int[]{Color.rgb(35,29,22),Color.rgb(83,62,38)}, () -> startActivity(new Intent(DashboardActivity.this, GpsRunActivity.class)));
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

    private void addActivityMode(int iconRes, String title, String subtitle, int[] colors, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(16), dp(13), dp(14), dp(13));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), Color.rgb(205, 155, 51));
        card.setBackground(bg);
        ImageView figure = new ImageView(this);
        figure.setImageResource(iconRes);
        figure.setColorFilter(Color.rgb(255, 199, 75));
        figure.setScaleType(ImageView.ScaleType.FIT_CENTER);
        figure.setElevation(dp(8));
        card.addView(figure, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(4), 0);
        TextView heading = text(title, WHITE, 23, true);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        info.addView(heading);
        info.addView(text(subtitle, GREY, 11, false));
        card.addView(info, new LinearLayout.LayoutParams(0,-2,1));
        TextView play = text("▶", Color.rgb(255, 216, 125), 20, true);
        play.setGravity(Gravity.CENTER);
        GradientDrawable playBg = new GradientDrawable();
        playBg.setColor(Color.rgb(12,17,27));
        playBg.setCornerRadius(dp(40));
        playBg.setStroke(dp(2), GOLD);
        play.setBackground(playBg);
        card.addView(play, new LinearLayout.LayoutParams(dp(49),dp(49)));
        card.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,dp(91));
        p.bottomMargin = dp(13);
        mainLayout.addView(card,p);
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

        addText(
                "Your running and workout centre.",
                GREY, 14, false
        );

        addActionCard(
                "START GPS RUN",
                "Record distance, pace, time and route",
                GREEN,
                () -> startActivity(new Intent(
                        DashboardActivity.this, GpsRunActivity.class
                ))
        );

        addActionCard(
                "PERFORMANCE ANALYTICS",
                "Charts, training totals, pace trends and GPS routes",
                GOLD,
                () -> startActivity(new Intent(DashboardActivity.this, PerformanceAnalyticsActivity.class))
        );

        addActionCard(
                "WORKOUT HISTORY",
                "View completed Fartlek sessions saved on this phone",
                BLUE,
                () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutHistoryActivity.class
                ))
        );

        addActionCard(
                "RUNNING REPORTS",
                "Create, download and share branded Q-BOIX PDF reports",
                GOLD,
                () -> startActivity(new Intent(
                        DashboardActivity.this, WorkoutReportsActivity.class
                ))
        );

        addActionCard(
                "PERSONAL RECORDS",
                "View your best performances",
                GOLD,
                () -> showPage("Records")
        );
    }

    // TRAINING

    private void showTraining() {

        pageHeading("TRAINING CENTRE");

        addText(
                "Choose the type of training you want to perform.",
                GREY, 13, false
        );

        addActionCard(
                "FARTLEK",
                "Custom sprint and recovery intervals",
                GOLD,
                () -> startActivity(new Intent(
                        DashboardActivity.this, FartlekActivity.class
                ))
        );

        addActionCard(
                "TRACK AND FIELD",
                "Track laps, split times and distances",
                BLUE,
                () -> showMessage(
                        "Track and field tools will be developed in Steps 12 and 14."
                )
        );

        addActionCard(
                "CROSS-COUNTRY",
                "Cross-country running and training",
                GREEN,
                () -> showMessage(
                        "Cross-country tracking will connect to the GPS engine."
                )
        );

        addActionCard(
                "ROAD RUNNING",
                "Road running and distance sessions",
                BLUE,
                () -> showMessage(
                        "Road running tools will be connected to the GPS engine."
                )
        );

        addActionCard(
                "MARATHON TRAINING",
                "Long-distance training sessions",
                GOLD,
                () -> showMessage(
                        "Marathon training will be developed in Step 14."
                )
        );

        addActionCard(
                "CUSTOM WORKOUT",
                "Create your own training session",
                GREY,
                () -> showMessage(
                        "Custom workouts will be added to the training system."
                )
        );
    }

    // RECORDS

    private void showRecords() {

        pageHeading("PERSONAL RECORDS");

        addText(
                "Your best performances and progress.",
                GREY, 14, false
        );

        String[] events = {
                "800 METRES",
                "1500 METRES",
                "3000 METRES",
                "5000 METRES",
                "10 KILOMETRES",
                "HALF MARATHON",
                "MARATHON"
        };

        for (String event : events) {
            addSimpleCard(event, "No recorded result yet");
        }
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
        card.setBackground(background(CARD, 13));

        TextView number = text(value, GOLD, 19, true);
        TextView title = text(label, GREY, 10, true);

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
