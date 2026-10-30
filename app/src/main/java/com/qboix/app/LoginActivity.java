package com.qboix.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class LoginActivity extends Activity {

    // Account data is kept in this app's private storage only (no cloud)
    private static final String PREFS = "qboix_account";
    private static final int MIN_CODE_LENGTH = 4;

    private SharedPreferences prefs;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#05060A"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/login.html");
        setContentView(web);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    // Methods the HTML page can call
    public class Bridge {

        @JavascriptInterface
        public boolean hasAccount() {
            return prefs.contains("hash");
        }

        @JavascriptInterface
        public String createAccount(String id, String code) {
            if (code == null || code.length() < MIN_CODE_LENGTH) {
                return "Access code must be at least 4 characters";
            }
            try {
                String salt = randomSalt();
                prefs.edit()
                        .putString("id", id)
                        .putString("salt", salt)
                        .putString("hash", hash(salt, code))
                        .apply();
                return "OK";
            } catch (Exception e) {
                return "Could not save the account";
            }
        }

        @JavascriptInterface
        public String signIn(String id, String code) {
            String savedId = prefs.getString("id", "");
            String salt = prefs.getString("salt", "");
            String savedHash = prefs.getString("hash", "");
            if (!id.equals(savedId)) {
                return "Unknown operator ID";
            }
            try {
                return hash(salt, code).equals(savedHash) ? "OK" : "Access denied";
            } catch (Exception e) {
                return "Sign-in failed";
            }
        }

        @JavascriptInterface
        public void openBoot() {
            runOnUiThread(() -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        }
    }

    private String randomSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    // SHA-256 of salt and code together. The access code itself is never stored.
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
