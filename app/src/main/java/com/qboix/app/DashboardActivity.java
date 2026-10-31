package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends Activity {

    private static final int GOLD = Color.rgb(215, 181, 91);
    private static final int BLACK = Color.rgb(9, 9, 13);
    private static final int CARD = Color.rgb(27, 27, 34);
    private static final int WHITE = Color.WHITE;
    private static final int GREY = Color.rgb(165, 165, 175);

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

        buildDashboard(user);
    }

    private void buildDashboard(FirebaseUser user) {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BLACK);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(22), dp(28), dp(22), dp(30));

        scrollView.addView(mainLayout);

        // BRAND HEADER

        TextView brand = new TextView(this);
        brand.setText("Q-BOIX");
        brand.setTextColor(GOLD);
        brand.setTextSize(29);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setLetterSpacing(0.12f);

        mainLayout.addView(brand);

        TextView tagline = new TextView(this);
        tagline.setText("DESIRE TO ACHIEVE");
        tagline.setTextColor(GREY);
        tagline.setTextSize(10);
        tagline.setLetterSpacing(0.22f);

        addView(mainLayout, tagline, 0, 12);

        // WELCOME SECTION

        TextView welcome = new TextView(this);

        String name = user.getDisplayName();

        if (name == null || name.trim().isEmpty()) {
            name = "Athlete";
        }

        welcome.setText("Welcome, " + name);
        welcome.setTextColor(WHITE);
        welcome.setTextSize(25);
        welcome.setTypeface(Typeface.DEFAULT_BOLD);

        addView(mainLayout, welcome, 0, 38);

        TextView introduction = new TextView(this);
        introduction.setText(
                "Your journey. Your performance. Your achievement."
        );
        introduction.setTextColor(GREY);
        introduction.setTextSize(13);

        addView(mainLayout, introduction, 0, 8);

        // PROFILE CARD

        LinearLayout profileCard = createCard();

        TextView profileTitle = createText(
                "ATHLETE PROFILE",
                GOLD,
                11,
                true
        );

        profileTitle.setLetterSpacing(0.12f);

        profileCard.addView(profileTitle);

        TextView email = createText(
                user.getEmail() == null ? "No email available" : user.getEmail(),
                WHITE,
                15,
                false
        );

        addView(profileCard, email, 0, 12);

        TextView accountStatus = createText(
                "EMAIL VERIFIED  ✓",
                GOLD,
                11,
                true
        );

        addView(profileCard, accountStatus, 0, 10);

        addCard(mainLayout, profileCard, 28);

        // TRAINING SECTION

        addSectionTitle(mainLayout, "YOUR ATHLETE SPACE");

        addFeatureCard(
                mainLayout,
                "01",
                "RUNNING",
                "Track your runs, distance, pace and routes.",
                "GPS RUN TRACKING"
        );

        addFeatureCard(
                mainLayout,
                "02",
                "TRAINING",
                "Create workouts, intervals and fartlek sessions.",
                "WORKOUT PLANNER"
        );

        addFeatureCard(
                mainLayout,
                "03",
                "PERFORMANCE",
                "Follow your personal records and training progress.",
                "ATHLETE ANALYTICS"
        );

        addFeatureCard(
                mainLayout,
                "04",
                "COMMUNITY",
                "Connect with other athletes and share achievements.",
                "ATHLETE NETWORK"
        );

        // ACCOUNT SECTION

        addSectionTitle(mainLayout, "ACCOUNT");

        Button logoutButton = new Button(this);

        logoutButton.setText("LOG OUT");
        logoutButton.setTextColor(BLACK);
        logoutButton.setTextSize(14);
        logoutButton.setTypeface(Typeface.DEFAULT_BOLD);
        logoutButton.setAllCaps(false);
        logoutButton.setBackground(createBackground(GOLD, 12));

        LinearLayout.LayoutParams logoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54)
                );

        logoutParams.topMargin = dp(12);

        mainLayout.addView(logoutButton, logoutParams);

        logoutButton.setOnClickListener(v -> confirmLogout());

        TextView footer = createText(
                "Q-BOIX  •  ONE VISION. MANY CREATIONS.",
                GREY,
                10,
                true
        );

        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.08f);

        addView(mainLayout, footer, 0, 28);

        setContentView(scrollView);
    }

    private void addSectionTitle(
            LinearLayout parent,
            String title
    ) {

        TextView heading = createText(
                title,
                GOLD,
                13,
                true
        );

        heading.setLetterSpacing(0.12f);

        addView(parent, heading, 0, 30);
    }

    private void addFeatureCard(
            LinearLayout parent,
            String number,
            String title,
            String description,
            String category
    ) {

        LinearLayout card = createCard();

        TextView numberText = createText(
                number,
                GOLD,
                12,
                true
        );

        card.addView(numberText);

        TextView titleText = createText(
                title,
                WHITE,
                19,
                true
        );

        addView(card, titleText, 0, 10);

        TextView descriptionText = createText(
                description,
                GREY,
                13,
                false
        );

        addView(card, descriptionText, 0, 7);

        TextView categoryText = createText(
                category + "  •  COMING SOON",
                GOLD,
                10,
                true
        );

        addView(card, categoryText, 0, 14);

        addCard(parent, card, 13);
    }

    private LinearLayout createCard() {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        card.setBackground(
                createBackground(CARD, 16)
        );

        return card;
    }

    private void addCard(
            LinearLayout parent,
            View card,
            int topMargin
    ) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dp(topMargin);

        parent.addView(card, params);
    }

    private void addView(
            LinearLayout parent,
            View child,
            int horizontalMargin,
            int topMargin
    ) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.leftMargin = dp(horizontalMargin);
        params.rightMargin = dp(horizontalMargin);
        params.topMargin = dp(topMargin);

        parent.addView(child, params);
    }

    private TextView createText(
            String text,
            int color,
            float size,
            boolean bold
    ) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable createBackground(
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

        new AlertDialog.Builder(this)
                .setTitle("Log out of Q-BOIX?")
                .setMessage(
                        "Are you sure you want to end your current session?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (dialog, which) -> {

                    auth.signOut();

                    Toast.makeText(
                            this,
                            "You have logged out.",
                            Toast.LENGTH_SHORT
                    ).show();

                    openLogin();
                })
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
