package com.stockmentor.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockmentor.security.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds
) {
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null
                || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero");
        }
    }
}
