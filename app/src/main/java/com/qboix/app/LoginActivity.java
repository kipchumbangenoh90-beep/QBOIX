package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends Activity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private CheckBox rememberCheckBox;
    private TextView passwordToggle;
    private Button signInButton;

    private FirebaseAuth auth;

    private boolean passwordVisible = false;
    private boolean navigating = false;

    private static final String PREFS = "qboix_login_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberCheckBox = findViewById(R.id.rememberCheckBox);
        passwordToggle = findViewById(R.id.passwordToggle);
        signInButton = findViewById(R.id.signInButton);

        loadRememberedEmail();

        passwordToggle.setOnClickListener(v -> togglePassword());

        signInButton.setOnClickListener(v -> login());

        findViewById(R.id.forgotPasswordText)
                .setOnClickListener(v -> showPasswordResetDialog());

        findViewById(R.id.signUpText)
                .setOnClickListener(v -> {
                    startActivity(new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    ));
                });

        findViewById(R.id.googleButton)
                .setOnClickListener(v ->
                        showMessage("Google sign-in is not configured yet.")
                );

        findViewById(R.id.appleButton)
                .setOnClickListener(v ->
                        showMessage("Apple sign-in is not configured yet.")
                );

        findViewById(R.id.facebookButton)
                .setOnClickListener(v ->
                        showMessage("Facebook sign-in is not configured yet.")
                );

        findViewById(R.id.quickLoginButton)
                .setOnClickListener(v ->
                        showMessage("Biometric login is not configured yet.")
                );
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || navigating) {
            return;
        }

        user.reload().addOnCompleteListener(this, task -> {

            if (isFinishing() || isDestroyed() || navigating) {
                return;
            }

            FirebaseUser refreshedUser = auth.getCurrentUser();

            if (task.isSuccessful()
                    && refreshedUser != null
                    && refreshedUser.isEmailVerified()) {

                openDashboard();

            } else if (task.isSuccessful()) {

                auth.signOut();
            }
        });
    }

    private void loadRememberedEmail() {

        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean remember = prefs.getBoolean("remember", false);

        rememberCheckBox.setChecked(remember);

        if (remember) {
            emailEditText.setText(
                    prefs.getString("operator", "")
            );
        }
    }

    private void togglePassword() {

        int cursor = passwordEditText.getSelectionStart();

        if (passwordVisible) {

            passwordEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            passwordToggle.setText("SHOW");
            passwordVisible = false;

        } else {

            passwordEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            passwordToggle.setText("HIDE");
            passwordVisible = true;
        }

        passwordEditText.setTypeface(Typeface.DEFAULT);

        passwordEditText.setSelection(
                Math.max(0, cursor)
        );
    }

    private void login() {

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        resetLoginButton();

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed. Please try again.";

                        showMessage(message);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        resetLoginButton();
                        showMessage("Unable to load your account.");
                        return;
                    }

                    user.reload().addOnCompleteListener(this, reloadTask -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        FirebaseUser refreshedUser = auth.getCurrentUser();

                        if (!reloadTask.isSuccessful()) {

                            auth.signOut();
                            resetLoginButton();

                            showMessage(
                                    "Could not verify your account. Check your internet connection and try again."
                            );

                            return;
                        }

                        if (refreshedUser != null
                                && refreshedUser.isEmailVerified()) {

                            saveRememberedEmail(email);

                            Toast.makeText(
                                    this,
                                    "Welcome to Q-BOIX!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            openDashboard();

                        } else {

                            sendVerificationAndSignOut(user);
                        }
                    });
                });
    }

    private void sendVerificationAndSignOut(FirebaseUser user) {

        user.sendEmailVerification()
                .addOnCompleteListener(this, task -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    auth.signOut();
                    resetLoginButton();

                    if (task.isSuccessful()) {

                        showMessage(
                                "Your email is not verified yet. We have sent you another verification link. Check your inbox and spam folder."
                        );

                    } else {

                        showMessage(
                                "Your email is not verified. Please check your inbox for the verification link and try again."
                        );
                    }
                });
    }

    private void resetLoginButton() {

        signInButton.setEnabled(true);
        signInButton.setText("Log In   →");
    }

    private void saveRememberedEmail(String email) {

        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS, MODE_PRIVATE).edit();

        editor.putBoolean(
                "remember",
                rememberCheckBox.isChecked()
        );

        if (rememberCheckBox.isChecked()) {
            editor.putString("operator", email);
        } else {
            editor.remove("operator");
        }

        editor.apply();
    }

    private void showPasswordResetDialog() {

        EditText emailInput = new EditText(this);

        emailInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        emailInput.setHint("Enter your registered email");

        emailInput.setText(
                emailEditText.getText().toString().trim()
        );

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage(
                        "We'll send a password reset link to your email."
                )
                .setView(emailInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Link", (dialog, which) -> {

                    String email =
                            emailInput.getText().toString().trim();

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        showMessage("Please enter a valid email address.");
                        return;
                    }

                    auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(this, task -> {

                                if (task.isSuccessful()) {

                                    showMessage(
                                            "If an account exists for this email, a password reset link has been sent."
                                    );

                                } else {

                                    showMessage(
                                            "Unable to send the reset email. Check your connection and try again."
                                    );
                                }
                            });
                })
                .show();
    }

    private void openDashboard() {

        if (navigating) {
            return;
        }

        navigating = true;

        Intent intent = new Intent(
                LoginActivity.this,
                DashboardActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showMessage(String message) {

        new AlertDialog.Builder(this)
                .setTitle("Q-BOIX")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
