package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends Activity {

    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;

    private CheckBox termsCheckBox;
    private Button createAccountButton;

    private FirebaseAuth auth;
    private boolean registrationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.registerEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        termsCheckBox = findViewById(R.id.termsCheckBox);
        createAccountButton = findViewById(R.id.createAccountButton);

        createAccountButton.setOnClickListener(v -> createAccount());

        findViewById(R.id.backToLoginText)
                .setOnClickListener(v -> finish());
    }

    private void createAccount() {

        if (registrationInProgress) {
            return;
        }

        String name =
                fullNameEditText.getText().toString().trim();

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        String confirmPassword =
                confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(name)) {
            fullNameEditText.setError("Enter your full name");
            fullNameEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(
                    "Password must have at least 6 characters"
            );
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(
                    "Passwords do not match"
            );
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(
                    this,
                    "Please accept the Terms and Privacy Policy",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        registrationInProgress = true;
        createAccountButton.setEnabled(false);
        createAccountButton.setText("Creating account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        finishRegistrationButton();

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed.";

                        showMessage(message);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {

                        auth.signOut();
                        finishRegistrationButton();

                        showMessage(
                                "Account created, but account details could not load. Please try logging in."
                        );

                        return;
                    }

                    updateUserProfile(user, name);
                });
    }

    private void updateUserProfile(
            FirebaseUser user,
            String name
    ) {

        UserProfileChangeRequest profile =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

        user.updateProfile(profile)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        auth.signOut();
                        finishRegistrationButton();

                        showMessage(
                                "Your account was created, but your profile could not be completed. Please contact support."
                        );

                        return;
                    }

                    sendVerificationEmail(user);
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {

        user.sendEmailVerification()
                .addOnCompleteListener(this, task -> {

                    auth.signOut();

                    finishRegistrationButton();

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Verification email sent. Check your inbox and spam folder.",
                                Toast.LENGTH_LONG
                        ).show();

                        Intent intent = new Intent(
                                RegisterActivity.this,
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        showMessage(
                                "Your account was created, but the verification email could not be sent. Please try logging in and requesting verification again."
                        );
                    }
                });
    }

    private void finishRegistrationButton() {

        registrationInProgress = false;

        createAccountButton.setEnabled(true);
        createAccountButton.setText("Create Account");
    }

    private void showMessage(String message) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Q-BOIX")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
