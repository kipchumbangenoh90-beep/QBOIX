package com.qboix.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Q-BOIX Health Connect entry point.
 * Users remain in control of every read/write permission through Android's
 * official Health Connect permission screen.
 */
public class HealthConnectActivity extends ComponentActivity {
    private static final int GOLD = Color.rgb(215, 181, 91);
    private static final int GREEN = Color.rgb(55, 190, 125);
    private static final int BLUE = Color.rgb(52, 136, 255);
    private static final int WHITE = Color.WHITE;
    private static final int GREY = Color.rgb(180, 185, 195);

    private static final String HC_PACKAGE = "com.google.android.apps.healthdata";
    private static final String READ_STEPS = "android.permission.health.READ_STEPS";
    private static final String READ_HEART = "android.permission.health.READ_HEART_RATE";
    private static final String READ_DISTANCE = "android.permission.health.READ_DISTANCE";
    private static final String READ_CALORIES = "android.permission.health.READ_TOTAL_CALORIES_BURNED";
    private static final String READ_EXERCISE = "android.permission.health.READ_EXERCISE";
    private static final String WRITE_EXERCISE = "android.permission.health.WRITE_EXERCISE";
    private static final String WRITE_DISTANCE = "android.permission.health.WRITE_DISTANCE";

    private final Set<String> requestedPermissions = new HashSet<>(Arrays.asList(
            READ_STEPS, READ_HEART, READ_DISTANCE, READ_CALORIES,
            READ_EXERCISE, WRITE_EXERCISE, WRITE_DISTANCE
    ));

    private TextView statusText;
    private ActivityResultLauncher<Set<String>> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                granted -> {
                    int count = 0;
                    for (String permission : requestedPermissions) {
                        if (granted.contains(permission)) count++;
                    }
                    if (count == requestedPermissions.size()) {
                        statusText.setText("Permissions granted • " + count + " of " + requestedPermissions.size());
                        Toast.makeText(this, "Health Connect permissions granted", Toast.LENGTH_LONG).show();
                    } else {
                        statusText.setText("Some permissions not granted • " + count + " of " + requestedPermissions.size());
                    }
                }
        );

        buildScreen();
        refreshAvailability();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(9, 11, 17));

        TextView brand = label("Q-BOIX", GOLD, 27, true);
        root.addView(brand);
        addSpace(root, 22);

        TextView title = label("CONNECTED APPS", WHITE, 23, true);
        root.addView(title);
        addSpace(root, 7);
        root.addView(label("HEALTH & FITNESS DATA HUB", GREEN, 11, true));
        addSpace(root, 18);

        LinearLayout statusCard = card();
        statusCard.addView(label("HEALTH CONNECT", BLUE, 14, true));
        statusText = label("Checking Health Connect availability…", WHITE, 14, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(9);
        statusCard.addView(statusText, statusParams);
        root.addView(statusCard);

        addSpace(root, 16);
        root.addView(label("SUPPORTED DATA TYPES", GOLD, 12, true));
        addSpace(root, 7);
        root.addView(label("• Steps and activity totals\n• Heart-rate records\n• Distance and exercise sessions\n• Total calories burned", GREY, 13, false));

        addSpace(root, 18);
        Button connect = button("CONNECT / MANAGE PERMISSIONS", GREEN);
        connect.setOnClickListener(v -> requestHealthPermissions());
        root.addView(connect, new LinearLayout.LayoutParams(-1, dp(52)));

        addSpace(root, 9);
        Button settings = button("OPEN HEALTH CONNECT SETTINGS", BLUE);
        settings.setOnClickListener(v -> openHealthConnectSettings());
        root.addView(settings, new LinearLayout.LayoutParams(-1, dp(50)));

        addSpace(root, 18);
        root.addView(label("HOW APP CONNECTIONS WORK", GOLD, 12, true));
        addSpace(root, 7);
        root.addView(label(
                "Health Connect is Android’s secure health-data hub. If Samsung Health or Strava is configured to share compatible records with Health Connect, Q-BOIX can request permission to access those records here. You control each permission and can revoke it at any time.",
                GREY, 12, false));

        addSpace(root, 13);
        root.addView(label(
                "GLORYFIT / QX11 WATCH\nYour QX11 watch remains connected through Q-BOIX’s existing Bluetooth integration. GloryFit is not automatically linked by Health Connect; direct GloryFit syncing depends on the companion app supporting Health Connect export.",
                GREY, 12, false));

        addSpace(root, 13);
        root.addView(label(
                "Privacy: Q-BOIX requests only the health-data types listed above. Granting permission does not mean another app is connected automatically; the other app must also share data through Health Connect.",
                GREY, 11, false));

        addSpace(root, 18);
        Button back = button("BACK TO Q-BOIX", GOLD);
        back.setOnClickListener(v -> finish());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));

        setContentView(root);
    }

    private void refreshAvailability() {
        int status = HealthConnectClient.getSdkStatus(this);
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            statusText.setText("Available on this phone. Choose which Q-BOIX data permissions to grant.");
        } else if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            statusText.setText("Health Connect needs to be installed or updated before Q-BOIX can connect.");
        } else {
            statusText.setText("Health Connect is not available on this Android device.");
        }
    }

    private void requestHealthPermissions() {
        int status = HealthConnectClient.getSdkStatus(this);
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            permissionLauncher.launch(requestedPermissions);
        } else if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            openHealthConnectStore();
        } else {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Health Connect unavailable")
                    .setMessage("This Android version does not support Health Connect.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void openHealthConnectSettings() {
        try {
            Intent intent = new Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS");
            startActivity(intent);
        } catch (Exception e) {
            openHealthConnectStore();
        }
    }

    private void openHealthConnectStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + HC_PACKAGE)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + HC_PACKAGE)));
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(25, 29, 39));
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(48, 66, 83));
        box.setBackground(bg);
        return box;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(8, 12, 18));
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(tint(color));
        return b;
    }

    private GradientDrawable tint(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(13));
        return d;
    }

    private TextView label(String text, int color, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void addSpace(LinearLayout parent, int height) {
        TextView spacer = new TextView(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
