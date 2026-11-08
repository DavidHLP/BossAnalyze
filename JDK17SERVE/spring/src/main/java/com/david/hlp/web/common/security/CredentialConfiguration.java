package com.david.hlp.web.common.security;

import java.util.Base64;
import java.util.Arrays;
import java.util.Locale;

/** Validates supplied configuration only; never generates or rotates credentials. */
public final class CredentialConfiguration {
    private CredentialConfiguration() { }

    private static boolean supplied(String value) {
        return value != null && !value.isBlank()
                && !value.contains("${")
                && !value.strip().toLowerCase(Locale.ROOT).startsWith("your_");
    }

    public static void requireSmtp(String username, String password) {
        if (!supplied(username) || !supplied(password)) {
            throw new IllegalStateException("SMTP configuration is required; configure spring.mail.username and spring.mail.password externally");
        }
    }

    public static void requireJwtSecret(String secret) {
        boolean valid = false;
        byte[] decoded = null;
        if (supplied(secret)) {
            try {
                decoded = Base64.getDecoder().decode(secret);
                valid = decoded.length >= 32;
            } catch (IllegalArgumentException ignored) {
                // Never attach decoder exceptions or the supplied value to diagnostics.
            } finally {
                if (decoded != null) Arrays.fill(decoded, (byte) 0);
            }
        }
        if (!valid) {
            throw new IllegalStateException("JWT configuration requires an externally supplied Base64 key of at least 32 bytes");
        }
    }
}
