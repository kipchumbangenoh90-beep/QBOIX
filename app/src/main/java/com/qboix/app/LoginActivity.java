package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class LoginActivity extends Activity {

    // Account data is kept in this app's private storage only (no cloud)
    private static final String PREFS = "qboix_account";
    private static final int MIN_CODE_LENGTH = 4;

    private SharedPreferences prefs;
    private boolean creating;
    private TextView title, message;
    private EditText idBox, codeBox, confirmBox;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        creating = !prefs.contains("hash");
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#020204"));
        root.setPadding(64, 48, 64, 48);

        title = new TextView(this);
        title.setText("Q-BOIX");
        title.setTextColor(Color.parseColor("#FFE4A0"));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView tag = new TextView(this);
        tag.setText("Desire to Achieve");
        tag.setTextColor(Color.parseColor("#D6A64A"));
        tag.setTextSize(13);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(0, 8, 0, 36);
        root.addView(tag);

        idBox = field("OPERATOR ID", false);
        root.addView(idBox);

        codeBox = field("ACCESS CODE (AT LEAST 4 CHARACTERS)", true);
        root.addView(codeBox);

        confirmBox = field("CONFIRM ACCESS CODE", true);
        confirmBox.setVisibility(creating ? View.VISIBLE : View.GONE);
        root.addView(confirmBox);

        actionButton = new Button(this);
        actionButton.setText(creating ? "CREATE ACCOUNT" : "SIGN IN");
        actionButton.setTextColor(Color.parseColor("#09090D"));
        actionButton.setBackgroundColor(Color.parseColor("#D6A64A"));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 32, 0, 0);
        actionButton.setLayoutParams(bp);
        actionButton.setOnClickListener(v -> {
            if (creating) createAccount(); else signIn();
        });
        root.addView(actionButton);

        message = new TextView(this);
        message.setTextColor(Color.parseColor("#52718A"));
        message.setTextSize(12);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 24, 0, 0);
        message.setText(creating
                ? "Create your operator account. It is stored only on this phone."
                : "Enter your operator ID and access code.");
        root.addView(message);

        setContentView(root);
    }

    private EditText field(String hint, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#52718A"));
        e.setTextColor(Color.parseColor("#A9D8FF"));
        e.setSingleLine(true);
        if (secret) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        }
        return e;
    }

    private void createAccount() {
        String id = idBox.getText().toString().trim().toUpperCase();
        String code = codeBox.getText().toString();
        String confirm = confirmBox.getText().toString();

        if (id.isEmpty()) {
            showError("ENTER AN OPERATOR ID");
            return;
        }
        if (code.length() < MIN_CODE_LENGTH) {
            showError("ACCESS CODE MUST BE AT LEAST 4 CHARACTERS");
            return;
        }
        if (!code.equals(confirm)) {
            showError("ACCESS CODES DO NOT MATCH");
            return;
        }

        try {
            String salt = randomSalt();
            String hash = hash(salt, code);
            prefs.edit()
                    .putString("id", id)
                    .putString("salt", salt)
                    .putString("hash", hash)
                    .apply();
            openBoot(id, "ACCOUNT CREATED");
        } catch (Exception e) {
            showError("COULD NOT SAVE ACCOUNT");
        }
    }

    private void signIn() {
        String id = idBox.getText().toString().trim().toUpperCase();
        String code = codeBox.getText().toString();

        String savedId = prefs.getString("id", "");
        String salt = prefs.getString("salt", "");
        String savedHash = prefs.getString("hash", "");

        if (!id.equals(savedId)) {
            showError("UNKNOWN OPERATOR ID");
            return;
        }
        try {
            if (hash(salt, code).equals(savedHash)) {
                openBoot(id, "ACCESS GRANTED");
            } else {
                showError("ACCESS DENIED");
                codeBox.setText("");
            }
        } catch (Exception e) {
            showError("SIGN-IN FAILED");
        }
    }

    private void openBoot(String id, String text) {
        message.setTextColor(Color.parseColor("#168BFF"));
        message.setText(text + ", " + id);
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void showError(String text) {
        message.setTextColor(Color.parseColor("#FF303B"));
        message.setText(text);
    }

    private String randomSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    // SHA-256 of salt and code together. The code itself is never stored.
    private String hash(String salt, String code) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
