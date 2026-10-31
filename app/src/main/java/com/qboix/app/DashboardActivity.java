package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    private FirebaseAuth auth;

    private final String[] tabs = {
            "Home", "Activities", "Training",
            "Community", "Profile"
    };

    private final String[] icons = {
            "⌂", "◉", "◷", "◎", "♙"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || !user.isEmailVerified()) {
            auth.signOut();
            openLogin();
            return;
        }

        createApplicationLayout();
        showPage("Home");
    }

    private void createApplicationLayout() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
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

            TextView icon = text(icons[i], GOLD, 21, true);
            icon.setGravity(Gravity.CENTER);

            TextView label = text(tabs[i], GREY, 10, true);
            label.setGravity(Gravity.CENTER);

            item.addView(icon);
            item.addView(label);

            item.setOnClickListener(v -> showPage(page));

            navigation.addView(item, new LinearLayout.LayoutParams(
                    0, -2, 1
            ));
        }

        root.addView(navigation);

        setContentView(root);
    }

    private void showPage(String page) {

        currentPage = page;
        mainLayout.removeAllViews();

        switch (page) {
            case "Home":
                showHome();
                break;

            case "Activities":
                showActivities();
                break;

            case "Training":
                showTraining();
                break;

            case "Community":
                showCommunity();
                break;

            case "Profile":
                showProfile();
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

            TextView icon = (TextView) item.getChildAt(0);
            TextView label = (TextView) item.getChildAt(1);

            icon.setTextColor(selected ? GOLD : GREY);
            label.setTextColor(selected ? GOLD : GREY);

            item.setBackground(selected
                    ? background(Color.rgb(38, 34, 25), 10)
                    : background(Color.TRANSPARENT, 10));
        }
    }

    // HOME DASHBOARD

    private void showHome() {

        FirebaseUser user = auth.getCurrentUser();

        String name = "Athlete";

        if (user != null && user.getDisplayName() != null
                && !user.getDisplayName().trim().isEmpty()) {
            name = user.getDisplayName();
        }

        addText("Q-BOIX", GOLD, 29, true);
        addText("DESIRE TO ACHIEVE", GREY, 10, true);

        addSpace(28);

        addText("WELCOME BACK,", GREY, 12, true);
        addText(name, WHITE, 27, true);

        addSpace(6);

        addText(
                "One vision. Many creations.",
                GOLD, 13, true
        );

        addText(
                "Your journey. Your performance. Your achievement.",
                GREY, 12, false
        );

        addSpace(26);

        addSection("TODAY'S PERFORMANCE");

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        addStat(stats, "0.00 km", "DISTANCE");
        addStat(stats, "00:00", "DURATION");

        addView(mainLayout, stats, 0);

        LinearLayout stats2 = new LinearLayout(this);
        stats2.setOrientation(LinearLayout.HORIZONTAL);

        addStat(stats2, "--", "AVG PACE");
        addStat(stats2, "0", "CALORIES");

        addView(mainLayout, stats2, 8);

        addSpace(12);

        addText(
                "No recorded activities yet.",
                GREY, 11, false
        );

        addSpace(18);

        addSection("YOUR TRAINING");

        addActionCard(
                "START RUNNING",
                "GPS running, distance, pace and route",
                GREEN,
                () -> showPage("Activities")
        );

        addActionCard(
                "FARTLEK TRAINING",
                "Build sprint and recovery intervals",
                BLUE,
                () -> showPage("Training")
        );

        addActionCard(
                "TRAINING PLANS",
                "Organise your running sessions",
                GOLD,
                () -> showPage("Training")
        );

        addSection("WEEKLY OVERVIEW");

        LinearLayout weekly = new LinearLayout(this);
        weekly.setOrientation(LinearLayout.HORIZONTAL);

        addStat(weekly, "0", "RUNS");
        addStat(weekly, "0.0 km", "DISTANCE");

        addView(mainLayout, weekly, 0);

        addSpace(8);

        addText(
                "Your weekly statistics will update when recorded workouts are connected.",
                GREY, 11, false
        );

        addSection("YOUR ATHLETE SPACE");

        addActionCard(
                "PERSONAL RECORDS",
                "Review your best running performances",
                GOLD,
                () -> showPage("Records")
        );

        addActionCard(
                "ACHIEVEMENTS",
                "Track your medals and milestones",
                GOLD,
                () -> showPage("Achievements")
        );

        addActionCard(
                "Q-BOIX COMMUNITY",
                "Connect with other athletes",
                BLUE,
                () -> showPage("Community")
        );

        addActionCard(
                "SETTINGS",
                "Manage your application preferences",
                GREY,
                () -> showPage("Settings")
        );
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
                () -> showMessage(
                        "The GPS tracking engine is scheduled for Step 9."
                )
        );

        addActionCard(
                "RUNNING HISTORY",
                "View your saved running sessions",
                BLUE,
                () -> showMessage(
                        "Workout history will be connected when local storage is developed."
                )
        );

        addActionCard(
                "RUNNING REPORTS",
                "Review your detailed performance reports",
                GOLD,
                () -> showMessage(
                        "Running reports will be developed in the reporting phase."
                )
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
                () -> showMessage(
                        "The configurable fartlek timer will be developed in Step 13."
                )
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

        FirebaseUser user = auth.getCurrentUser();

        pageHeading("ATHLETE PROFILE");

        String name = user != null
                && user.getDisplayName() != null
                ? user.getDisplayName()
                : "Athlete";

        String email = user != null
                && user.getEmail() != null
                ? user.getEmail()
                : "No email";

        addText(name, WHITE, 25, true);
        addText(email, GREY, 13, false);

        addSpace(20);

        addSimpleCard(
                "ACCOUNT STATUS",
                "Email verified"
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
                    auth.signOut();
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
