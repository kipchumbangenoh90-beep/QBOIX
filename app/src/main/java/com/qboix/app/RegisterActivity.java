package com.qboix.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
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

        findViewById(R.id.backToLoginText).setOnClickListener(v -> finish());
    }

    private void createAccount() {

        String name = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(name)) {
            fullNameEditText.setError("Enter your full name");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must have at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Please accept the Terms and Privacy Policy",
                    Toast.LENGTH_LONG).show();
            return;
        }

        createAccountButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user == null) {
                            createAccountButton.setEnabled(true);
                            Toast.makeText(this, "Account created, but user details could not load.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        UserProfileChangeRequest profile =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                        user.updateProfile(profile)
                                .addOnCompleteListener(profileTask -> {

                                    if (!profileTask.isSuccessful()) {
                                        createAccountButton.setEnabled(true);
                                        Toast.makeText(this,
                                                "Account created, but profile setup failed. Please try again.",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    user.sendEmailVerification()
                                            .addOnCompleteListener(verificationTask -> {

                                                createAccountButton.setEnabled(true);

                                                if (verificationTask.isSuccessful()) {
                                                    Toast.makeText(this,
                                                            "Verification email sent. Verify your email before logging in.",
                                                            Toast.LENGTH_LONG).show();

                                                    auth.signOut();
                                                    startActivity(new Intent(
                                                            RegisterActivity.this,
                                                            LoginActivity.class));
                                                    finish();

                                                } else {
                                                    Toast.makeText(this,
                                                            "Account created, but verification email failed. Please try again.",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                });

                    } else {
                        createAccountButton.setEnabled(true);

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed.";

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
