package com.qboix.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class LocalAccountStore {
    private static final String PREFS = "qboix_local_accounts";
    private static final String SESSION = "qboix_local_session";
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private LocalAccountStore() {}

    private static SharedPreferences accounts(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized boolean create(Context c, String name, String email, String password) {
        String key = email.trim().toLowerCase(java.util.Locale.ROOT);
        SharedPreferences p = accounts(c);
        if (p.contains(key + ".hash")) return false;
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltText = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hash = derive(password.toCharArray(), salt);
            boolean saved = p.edit()
                    .putString(key + ".name", name.trim())
                    .putString(key + ".salt", saltText)
                    .putString(key + ".hash", hash)
                    .commit();
            if (saved) signIn(c, key);
            return saved;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean signIn(Context c, String email, String password) {
        String key = email.trim().toLowerCase(java.util.Locale.ROOT);
        SharedPreferences p = accounts(c);
        try {
            String saltText = p.getString(key + ".salt", null);
            String expected = p.getString(key + ".hash", null);
            if (saltText == null || expected == null) return false;
            String actual = derive(password.toCharArray(), Base64.decode(saltText, Base64.NO_WRAP));
            if (!constantTimeEquals(expected, actual)) return false;
            signIn(c, key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String derive(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void signIn(Context c, String emailKey) {
        c.getSharedPreferences(SESSION, Context.MODE_PRIVATE)
                .edit().putString("email", emailKey).commit();
    }

    public static void signOut(Context c) {
        c.getSharedPreferences(SESSION, Context.MODE_PRIVATE).edit().clear().commit();
    }

    public static boolean isSignedIn(Context c) {
        return getEmail(c) != null;
    }

    public static String getEmail(Context c) {
        return c.getSharedPreferences(SESSION, Context.MODE_PRIVATE)
                .getString("email", null);
    }

    public static String getName(Context c) {
        String email = getEmail(c);
        if (email == null) return "Athlete";
        return accounts(c).getString(email + ".name", "Athlete");
    }
}
