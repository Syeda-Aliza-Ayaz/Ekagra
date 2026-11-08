package com.ekagra.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2-HMAC-SHA256 password hashing with a random per-user salt (NFR-2). */
public final class PasswordHasher {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt),
                ITERATIONS, KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    public static boolean verify(String password, String salt, String expectedHash) {
        byte[] actual = hash(password, salt).getBytes();
        byte[] expected = expectedHash.getBytes();
        return MessageDigest.isEqual(actual, expected); // constant-time comparison
    }
}
