package com.stockmentor.auth.validation;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int BCRYPT_MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isBcryptCompatible(CharSequence password) {
        return password == null
                || password.toString().getBytes(StandardCharsets.UTF_8).length
                <= BCRYPT_MAX_UTF8_BYTES;
    }
}
