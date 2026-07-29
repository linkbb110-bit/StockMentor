package com.stockmentor.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.dto.UpdateNicknameRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void registrationAcceptsAPasswordContainingLettersAndDigits() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "study123", "学习投资");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registrationRejectsALettersOnlyPassword() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "password", "学习投资");

        assertThat(violatedProperties(request)).containsExactly("password");
    }

    @Test
    void registrationRejectsADigitsOnlyPassword() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "12345678", "学习投资");

        assertThat(violatedProperties(request)).containsExactly("password");
    }

    @Test
    void registrationRejectsAPasswordAbove64Characters() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "a1" + "x".repeat(63), "学习投资");

        assertThat(request.password()).hasSize(65);
        assertThat(violatedProperties(request)).containsExactly("password");
    }

    @Test
    void registrationDoesNotRejectEmailBeforeNormalization() {
        RegisterRequest request =
                new RegisterRequest(" Test@Example.COM ", "study123", "学习投资");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registrationDoesNotRejectNicknameBeforeNormalization() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "study123", "  长期学习者  ");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nicknameUpdateDoesNotRejectNicknameBeforeNormalization() {
        UpdateNicknameRequest request = new UpdateNicknameRequest("  长期学习者  ");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registrationRequiresEmailPresence() {
        RegisterRequest request = new RegisterRequest(null, "study123", "学习投资");

        assertThat(violatedProperties(request)).containsExactly("email");
    }

    @Test
    void registrationRequiresNicknamePresence() {
        RegisterRequest request =
                new RegisterRequest("student@example.com", "study123", null);

        assertThat(violatedProperties(request)).containsExactly("nickname");
    }

    @Test
    void nicknameUpdateRequiresNicknamePresence() {
        UpdateNicknameRequest request = new UpdateNicknameRequest(null);

        assertThat(violatedProperties(request)).containsExactly("nickname");
    }

    @Test
    void loginDoesNotRejectEmailBeforeNormalization() {
        LoginRequest request = new LoginRequest(" Test@Example.COM ", "study123");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void loginAllowsAnExistingLettersOnlyPassword() {
        LoginRequest request = new LoginRequest("student@example.com", "password");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void loginRequiresEmailPresence() {
        LoginRequest request = new LoginRequest(null, "study123");

        assertThat(violatedProperties(request)).containsExactly("email");
    }

    @Test
    void loginRequiresAnEightTo64CharacterPassword() {
        LoginRequest request = new LoginRequest("student@example.com", "short");

        assertThat(violatedProperties(request)).containsExactly("password");
    }

    @Test
    void authenticationAndUserErrorCodesExposeTheirRequiredHttpClassifications() {
        assertThat(ErrorCode.AUTH_INVALID_CREDENTIALS.httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.AUTH_INVALID_TOKEN.httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.AUTH_TOKEN_EXPIRED.httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.AUTH_USER_DISABLED.httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.AUTH_USER_NOT_FOUND.httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.USER_EMAIL_ALREADY_EXISTS.httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.USER_NICKNAME_INVALID.httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.FORBIDDEN.httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Set<String> violatedProperties(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
