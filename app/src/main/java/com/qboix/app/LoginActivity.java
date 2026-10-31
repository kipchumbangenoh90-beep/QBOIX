package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private EditText emailEditText, passwordEditText;
    private CheckBox rememberCheckBox;
    private TextView passwordToggle;
    private Button signInButton;
    private boolean passwordVisible = false;
    private boolean navigating = false;
    private static final String PREFS = "qboix_login_prefs";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LocalAccountStore.isSignedIn(this)) { openDashboard(); return; }
        setContentView(R.layout.activity_login);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberCheckBox = findViewById(R.id.rememberCheckBox);
        passwordToggle = findViewById(R.id.passwordToggle);
        signInButton = findViewById(R.id.signInButton);
        loadRememberedEmail();
        passwordToggle.setOnClickListener(v -> togglePassword());
        signInButton.setOnClickListener(v -> login());
        findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                showMessage("Password reset is managed on this phone. If you forget your password, you will need to create a new local account."));
        findViewById(R.id.signUpText).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.googleButton).setOnClickListener(v ->
                showMessage("Q-BOIX accounts are stored locally on this phone. Online sign-in is disabled."));
        findViewById(R.id.appleButton).setOnClickListener(v ->
                showMessage("Q-BOIX uses local phone accounts."));
        findViewById(R.id.facebookButton).setOnClickListener(v ->
                showMessage("Q-BOIX uses local phone accounts."));
        findViewById(R.id.quickLoginButton).setOnClickListener(v ->
                showMessage("Biometric login is not enabled yet."));
    }

    private void loadRememberedEmail() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean remember = p.getBoolean("remember", false);
        rememberCheckBox.setChecked(remember);
        if (remember) emailEditText.setText(p.getString("operator", ""));
    }

    private void togglePassword() {
        int cursor = passwordEditText.getSelectionStart();
        if (passwordVisible) {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordToggle.setText("SHOW"); passwordVisible = false;
        } else {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordToggle.setText("HIDE"); passwordVisible = true;
        }
        passwordEditText.setTypeface(Typeface.DEFAULT);
        passwordEditText.setSelection(Math.max(0, cursor));
    }

    private void login() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address"); emailEditText.requestFocus(); return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Enter your password"); passwordEditText.requestFocus(); return;
        }
        signInButton.setEnabled(false); signInButton.setText("Signing in...");
        if (LocalAccountStore.signIn(this, email, password)) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("remember", rememberCheckBox.isChecked())
                    .putString("operator", rememberCheckBox.isChecked() ? email : "").apply();
            Toast.makeText(this, "Welcome to Q-BOIX!", Toast.LENGTH_SHORT).show();
            openDashboard();
        } else {
            signInButton.setEnabled(true); signInButton.setText("Log In   →");
            showMessage("Account not found or password is incorrect. Create an account first if this is your first time on this phone.");
        }
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this).setTitle("Q-BOIX").setMessage(message)
                .setPositiveButton("OK", null).show();
    }

    private void openDashboard() {
        if (navigating) return;
        navigating = true;
        Intent i = new Intent(this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }
}
