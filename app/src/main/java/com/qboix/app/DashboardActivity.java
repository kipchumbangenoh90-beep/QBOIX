package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
        navigation.setPadding(dp(4), dp(8), dp(4), dp(8));
        navigation.setBackgroundColor(Color.rgb(17, 17, 23));

        String[] tabs = {
                "Home", "Activities", "Training",
                "Community", "Profile"
        };

        String[] icons = {
                "⌂", "◉", "◷", "◎", "♙"
        };

        for (int i = 0; i < tabs.length; i++) {

            final String page = tabs[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(7), dp(2), dp(7));

            TextView icon = text(
                    icons[i],
                    GOLD,
                    21,
                    true
            );

            icon.setGravity(Gravity.CENTER);

            TextView label = text(
                    tabs[i],
                    GREY,
                    10,
                    true
            );

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
    }

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

        addText("Welcome back,", GREY, 15, false);
        addText(name, WHITE, 27, true);

        addText(
                "Your journey. Your performance. Your achievement.",
                GREY, 12, false
        );

        addSpace(25);

        addText("TODAY'S PERFORMANCE", GOLD, 13, true);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        addStat(stats, "0.00 km", "DISTANCE");
        addStat(stats, "00:00", "DURATION");

        addView(mainLayout, stats, 16);

        LinearLayout stats2 = new LinearLayout(this);
        stats2.setOrientation(LinearLayout.HORIZONTAL);

        addStat(stats2, "0:00/km", "AVG PACE");
        addStat(stats2, "0", "CALORIES");

        addView(mainLayout, stats2, 10);

        addSpace(25);

        addActionCard(
                "START RUNNING",
                "Record a GPS running activity",
                GREEN,
                () -> showPage("Activities")
        );

        addActionCard(
                "FARTLEK TRAINING",
                "Create sprint and recovery intervals",
                BLUE,
                () -> showPage("Training")
        );

        addSection("YOUR ATHLETE SPACE");

        addActionCard(
                "PERSONAL RECORDS",
                "View your running performance",
                GOLD,
                () -> showPage("Records")
        );

        addActionCard(
                "ACHIEVEMENTS",
                "Your medals and milestones",
                GOLD,
                () -> showPage("Achievements")
        );

        addActionCard(
                "SETTINGS",
                "Manage your application",
                GREY,
                () -> showPage("Settings")
        );
    }

    private void showActivities() {

        pageHeading("ACTIVITIES");

        addText(
                "Your running and workout history will appear here.",
                GREY, 14, false
        );

        addSpace(20);

        addActionCard(
                "START GPS RUN",
                "Record distance, pace, time and route",
                GREEN,
                () -> showMessage(
                        "GPS running will be implemented in Step 9."
                )
        );

        addActionCard(
                "RUNNING HISTORY",
                "View your saved running sessions",
                BLUE,
                () -> showMessage(
                        "Running history will be connected when local workout storage is built."
                )
        );
    }

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
                        "The editable fartlek timer is coming in Step 13."
                )
        );

        addActionCard(
                "TRACK AND FIELD",
                "Track laps, split times and distances",
                BLUE,
                () -> showMessage(
                        "Track and field tools are coming in Steps 12 and 14."
                )
        );

        addActionCard(
                "CROSS-COUNTRY",
                "Cross-country running and training",
                GREEN,
                () -> showMessage(
                        "Cross-country tracking will be connected to GPS."
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
                "5 KILOMETRES",
                "10 KILOMETRES",
                "HALF MARATHON",
                "MARATHON"
        };

        for (String event : events) {
            addSimpleCard(event, "No recorded result yet");
        }
    }

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
                "ACHIEVEMENTS",
                "View medals and milestones",
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
    }

    private void pageHeading(String heading) {

        addText("Q-BOIX", GOLD, 25, true);
        addSpace(22);
        addText(heading, WHITE, 23, true);
        addSpace(8);
    }

    private void addSection(String heading) {

        addSpace(25);
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
        card.setPadding(dp(12), dp(18), dp(12), dp(18));
        card.setBackground(background(CARD, 13));

        TextView number = text(value, GOLD, 20, true);
        TextView title = text(label, GREY, 10, true);

        card.addView(number);
        card.addView(title);

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

        mainLayout.addView(space,
                new LinearLayout.LayoutParams(1, dp(size)));
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
\
