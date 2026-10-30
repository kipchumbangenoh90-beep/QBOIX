
package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private CheckBox rememberCheckBox;
    private TextView passwordToggle;
    private Button signInButton;

    private boolean passwordVisible = false;

    private static final String PREFS = "qboix_login_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberCheckBox = findViewById(R.id.rememberCheckBox);
        passwordToggle = findViewById(R.id.passwordToggle);
        signInButton = findViewById(R.id.signInButton);

        loadRememberedOperator();

        passwordToggle.setOnClickListener(v -> togglePassword());

        signInButton.setOnClickListener(v -> login());

        findViewById(R.id.forgotPasswordText)
                .setOnClickListener(v -> showMessage(
                        "Password recovery is coming soon."));

        findViewById(R.id.signUpText)
                .setOnClickListener(v -> showMessage(
                        "Q-BOIX account registration is coming soon."));

        findViewById(R.id.googleButton)
                .setOnClickListener(v -> showMessage(
                        "Google sign-in is not configured yet."));

        findViewById(R.id.appleButton)
                .setOnClickListener(v -> showMessage(
                        "Apple sign-in is not configured yet."));

        findViewById(R.id.facebookButton)
                .setOnClickListener(v -> showMessage(
                        "Facebook sign-in is not configured yet."));

        findViewById(R.id.quickLoginButton)
                .setOnClickListener(v -> showMessage(
                        "Biometric login is coming soon."));
    }

    private void loadRememberedOperator() {
        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean remember =
                prefs.getBoolean("remember", false);

        rememberCheckBox.setChecked(remember);

        if (remember) {
            emailEditText.setText(
                    prefs.getString("operator", ""));
        }
    }

    private void togglePassword() {

        int cursor = passwordEditText.getSelectionStart();

        if (passwordVisible) {

            passwordEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_PASSWORD);

            passwordToggle.setText("SHOW");

            passwordVisible = false;

        } else {

            passwordEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

            passwordToggle.setText("HIDE");

            passwordVisible = true;
        }

        passwordEditText.setTypeface(Typeface.DEFAULT);

        passwordEditText.setSelection(
                Math.max(0, cursor));
    }

    private void login() {

        String operator =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        if (operator.isEmpty()) {
            emailEditText.setError(
                    "Enter your email or phone number");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError(
                    "Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 4) {
            passwordEditText.setError(
                    "Minimum 4 characters required");
            passwordEditText.requestFocus();
            return;
        }

        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS, MODE_PRIVATE).edit();

        editor.putBoolean(
                "remember", rememberCheckBox.isChecked());

        if (rememberCheckBox.isChecked()) {
            editor.putString("operator", operator);
        } else {
            editor.remove("operator");
        }

        editor.apply();

        Toast.makeText(
                this,
                "Login details accepted.",
                Toast.LENGTH_SHORT
        ).show();

        // DEMO ONLY: connect real authentication before release.
        startActivity(
                new Intent(LoginActivity.this, MainActivity.class));
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Q-BOIX")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
