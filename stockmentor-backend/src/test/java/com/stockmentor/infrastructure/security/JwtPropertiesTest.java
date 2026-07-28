package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtProperties("", 7200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSecretShorterThanThirtyTwoUtf8Bytes() {
        assertThatThrownBy(() -> new JwtProperties("too-short", 7200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveExpiration() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsValidConfiguration() {
        JwtProperties properties = new JwtProperties("a".repeat(32), 7200);

        assertThat(properties.expirationSeconds()).isEqualTo(7200);
    }
}
