package com.qboix.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {
    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button createAccountButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
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
        String confirm = confirmPasswordEditText.getText().toString();
        if (TextUtils.isEmpty(name)) { fullNameEditText.setError("Enter your full name"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailEditText.setError("Enter a valid email address"); return; }
        if (password.length() < 8) { passwordEditText.setError("Use at least 8 characters"); return; }
        if (!password.equals(confirm)) { confirmPasswordEditText.setError("Passwords do not match"); return; }
        if (!termsCheckBox.isChecked()) { Toast.makeText(this, "Please accept the Terms and Privacy Policy", Toast.LENGTH_LONG).show(); return; }
        createAccountButton.setEnabled(false); createAccountButton.setText("Creating account...");
        boolean created = LocalAccountStore.create(this, name, email, password);
        if (!created) {
            createAccountButton.setEnabled(true); createAccountButton.setText("Create Account");
            showMessage("An account with this email already exists on this phone, or the account could not be saved.");
            return;
        }
        Toast.makeText(this, "Account created. Welcome to Q-BOIX!", Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this).setTitle("Q-BOIX").setMessage(message)
                .setPositiveButton("OK", null).show();
    }
}
