package com.stockmentor.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailNormalizerTest {

    private EmailNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new EmailNormalizer();
    }

    @Test
    void trimsAndLowercasesAValidMixedCaseEmail() {
        assertThat(normalizer.normalize(" Test@Example.COM "))
                .isEqualTo("test@example.com");
    }

    @Test
    void acceptsAValidNormalizedEmailAtThe254CharacterBoundary() {
        String emailAtBoundary = "a".repeat(64)
                + "@"
                + "b".repeat(63)
                + "."
                + "c".repeat(63)
                + "."
                + "d".repeat(61);

        assertThat(emailAtBoundary).hasSize(254);
        assertThat(normalizer.normalize(" " + emailAtBoundary.toUpperCase() + " "))
                .isEqualTo(emailAtBoundary);
    }

    @Test
    void rejectsAnOtherwiseValidNormalizedEmailAbove254Characters() {
        String emailAboveBoundary = "a".repeat(65)
                + "@"
                + "b".repeat(63)
                + "."
                + "c".repeat(63)
                + "."
                + "d".repeat(61);

        assertThat(emailAboveBoundary).hasSize(255);
        assertValidationFailure(emailAboveBoundary);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "plain-address",
        "student@localhost",
        "student@-example.com",
        "student@example..com",
        "student@example.com trailing"
    })
    void rejectsInvalidEmailFormats(String rawEmail) {
        assertValidationFailure(rawEmail);
    }

    @Test
    void rejectsNullEmailWithValidationFailed() {
        assertValidationFailure(null);
    }

    private void assertValidationFailure(String rawEmail) {
        assertThatThrownBy(() -> normalizer.normalize(rawEmail))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
