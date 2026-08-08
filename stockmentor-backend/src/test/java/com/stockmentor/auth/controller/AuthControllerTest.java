package com.stockmentor.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmentor.auth.dto.LoginRequest;
import com.stockmentor.auth.dto.RegisterRequest;
import com.stockmentor.auth.service.AuthenticationService;
import com.stockmentor.auth.vo.AuthResponse;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.common.exception.GlobalExceptionHandler;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.mapper.UserMapper;
import com.stockmentor.user.vo.CurrentUserResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String EMAIL = "student@example.com";
    private static final String MISSING_EMAIL = "missing@example.com";
    private static final String NICKNAME = "学习投资";
    private static final String TEST_PASSWORD = "test-only9";
    private static final String WRONG_PASSWORD = "wrong-only9";
    private static final String TEST_TOKEN = "test.jwt.placeholder";
    private static final String EXACT_BCRYPT_LIMIT_PASSWORD =
            "a1" + "学".repeat(23) + "x";
    private static final String ABOVE_BCRYPT_LIMIT_PASSWORD =
            EXACT_BCRYPT_LIMIT_PASSWORD + "x";
    private static final String REGISTER_LOG_PASSWORD =
            "REGISTER_PASSWORD9_SENTINEL_ALPHA";
    private static final String REGISTER_LOG_TOKEN =
            "REGISTER_TOKEN_SENTINEL_ALPHA";
    private static final String LOGIN_LOG_PASSWORD =
            "LOGIN_PASSWORD9_SENTINEL_BRAVO";
    private static final String LOGIN_LOG_TOKEN =
            "LOGIN_TOKEN_SENTINEL_BRAVO";
    private static final String VALIDATION_LOG_PASSWORD =
            "VALIDATION_PASSWORD_SENTINEL_CHARLIE";
    private static final String REGISTER_TO_STRING_PASSWORD =
            "REGISTER_PASSWORD9_SENTINEL_DELTA";
    private static final String LOGIN_TO_STRING_PASSWORD =
            "LOGIN_PASSWORD9_SENTINEL_ECHO";
    private static final String AUTH_TO_STRING_TOKEN =
            "AUTH_TOKEN_SENTINEL_FOXTROT";
    private static final String MVC_ANNOTATION_LOGGER =
            "org.springframework.web.servlet.mvc.method.annotation";
    private static final String MVC_ANNOTATION_LOGGER_PROPERTY =
            "logging.level." + MVC_ANNOTATION_LOGGER;
    private static final String INVALID_CREDENTIALS_JSON = """
            {
              "code": "AUTH_INVALID_CREDENTIALS",
              "message": "邮箱或密码错误",
              "data": null
            }
            """;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserMapper userMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    private MockMvc databaseExceptionMockMvc;

    @BeforeEach
    void setUpDatabaseExceptionController() {
        databaseExceptionMockMvc = MockMvcBuilders
                .standaloneSetup(new DatabaseExceptionTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validRegistrationReturnsCreatedAuthenticationWithoutSensitiveFields()
            throws Exception {
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(successfulAuthentication());

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, TEST_PASSWORD, NICKNAME)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        successfulAuthenticationJson("注册成功"),
                        JsonCompareMode.STRICT
                ))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.user.status").doesNotExist())
                .andExpect(jsonPath("$.data.user.deleted").doesNotExist())
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));
    }

    @Test
    void registrationRejectsPasswordWithoutADigitAsValidationFailure()
            throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, "test-only", NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": "VALIDATION_FAILED",
                          "message": "请求参数不合法",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void registrationAcceptsExactly72Utf8PasswordBytes() throws Exception {
        assertThat(EXACT_BCRYPT_LIMIT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .hasSize(72);
        when(authenticationService.register(new RegisterRequest(
                EMAIL,
                EXACT_BCRYPT_LIMIT_PASSWORD,
                NICKNAME
        ))).thenReturn(successfulAuthentication());

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(
                                EMAIL,
                                EXACT_BCRYPT_LIMIT_PASSWORD,
                                NICKNAME
                        )))
                .andExpect(status().isCreated());
    }

    @Test
    void registrationRejectsMoreThan72Utf8PasswordBytesAsStablePublic400(
            CapturedOutput output
    ) throws Exception {
        assertThat(ABOVE_BCRYPT_LIMIT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .hasSize(73);

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(
                                EMAIL,
                                ABOVE_BCRYPT_LIMIT_PASSWORD,
                                NICKNAME
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(validationFailureJson(), JsonCompareMode.STRICT));

        verifyNoInteractions(authenticationService);
        assertThat(output.getAll()).doesNotContain(ABOVE_BCRYPT_LIMIT_PASSWORD);
    }

    @Test
    void registrationReturnsBadRequestForNicknameRejectedAfterNormalization()
            throws Exception {
        RegisterRequest request =
                new RegisterRequest(EMAIL, TEST_PASSWORD, "!");
        when(authenticationService.register(request))
                .thenThrow(new BusinessException(ErrorCode.USER_NICKNAME_INVALID));

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, TEST_PASSWORD, "!")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": "USER_NICKNAME_INVALID",
                          "message": "昵称格式不合法",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void registrationReturnsConflictWhenEmailWasAlreadyFound()
            throws Exception {
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.USER_EMAIL_ALREADY_EXISTS
                ));

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(EMAIL, TEST_PASSWORD, NICKNAME)))
                .andExpect(status().isConflict())
                .andExpect(content().json(emailConflictJson(), JsonCompareMode.STRICT));
    }

    @Test
    void duplicateKeyRaceIsMappedToThePublicEmailConflict()
            throws Exception {
        databaseExceptionMockMvc.perform(post("/test/duplicate-key"))
                .andExpect(status().isConflict())
                .andExpect(content().json(emailConflictJson(), JsonCompareMode.STRICT));
    }

    @Test
    void nestedEmailUniqueConstraintViolationIsMappedToThePublicConflict()
            throws Exception {
        databaseExceptionMockMvc.perform(post(
                        "/test/nested-email-constraint"
                ))
                .andExpect(status().isConflict())
                .andExpect(content().json(emailConflictJson(), JsonCompareMode.STRICT));
    }

    @Test
    void unrelatedDataIntegrityViolationIsNotReportedAsEmailConflict()
            throws Exception {
        databaseExceptionMockMvc.perform(post(
                        "/test/unrelated-integrity-constraint"
                ))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {
                          "code": "INTERNAL_ERROR",
                          "message": "服务器内部错误",
                          "data": null
                        }
                        """, JsonCompareMode.STRICT))
                .andExpect(content().string(
                        not(containsString("uk_other_constraint"))
                ));
    }

    @Test
    void validLoginReturnsOkAuthenticationWithoutSensitiveFields()
            throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(successfulAuthentication());

        mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        successfulAuthenticationJson("登录成功"),
                        JsonCompareMode.STRICT
                ))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.user.status").doesNotExist())
                .andExpect(jsonPath("$.data.user.deleted").doesNotExist())
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));
    }

    @Test
    void loginAcceptsExactly72Utf8PasswordBytes() throws Exception {
        assertThat(EXACT_BCRYPT_LIMIT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .hasSize(72);
        when(authenticationService.login(new LoginRequest(
                EMAIL,
                EXACT_BCRYPT_LIMIT_PASSWORD
        ))).thenReturn(successfulAuthentication());

        mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, EXACT_BCRYPT_LIMIT_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void loginOver72Utf8BytesReturnsTheSamePublic400ForAnyEmail(
            CapturedOutput output
    ) throws Exception {
        assertThat(ABOVE_BCRYPT_LIMIT_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .hasSize(73);

        MvcResult existingEmailResult = mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, ABOVE_BCRYPT_LIMIT_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(validationFailureJson(), JsonCompareMode.STRICT))
                .andReturn();
        MvcResult missingEmailResult = mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(MISSING_EMAIL, ABOVE_BCRYPT_LIMIT_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(validationFailureJson(), JsonCompareMode.STRICT))
                .andReturn();

        assertThat(existingEmailResult.getResponse().getContentAsString())
                .isEqualTo(missingEmailResult.getResponse().getContentAsString());
        verifyNoInteractions(authenticationService);
        assertThat(output.getAll()).doesNotContain(ABOVE_BCRYPT_LIMIT_PASSWORD);
    }

    @Test
    void missingEmailAndWrongPasswordReturnTheSamePublicFailure()
            throws Exception {
        when(authenticationService.login(
                new LoginRequest(MISSING_EMAIL, TEST_PASSWORD)
        )).thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        when(authenticationService.login(
                new LoginRequest(EMAIL, WRONG_PASSWORD)
        )).thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        MvcResult missingEmailResult = mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(MISSING_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(
                        INVALID_CREDENTIALS_JSON,
                        JsonCompareMode.STRICT
                ))
                .andReturn();
        MvcResult wrongPasswordResult = mockMvc.perform(post(LOGIN_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(
                        INVALID_CREDENTIALS_JSON,
                        JsonCompareMode.STRICT
                ))
                .andReturn();

        String missingEmailResponse = missingEmailResult.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String wrongPasswordResponse = wrongPasswordResult.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(missingEmailResponse)
                .isEqualTo(wrongPasswordResponse)
                .doesNotContain(
                        MISSING_EMAIL,
                        EMAIL,
                        TEST_PASSWORD,
                        WRONG_PASSWORD
                );
    }

    @Test
    void validRegistrationDoesNotWriteSecretsToDebugLogs(
            CapturedOutput output
    ) throws Exception {
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(successfulAuthentication(REGISTER_LOG_TOKEN));

        Logger mvcLogger = mvcAnnotationLogger();
        Level originalLevel = mvcLogger.getLevel();
        assertThat(originalLevel).isEqualTo(Level.INFO);
        try {
            mvcLogger.setLevel(Level.DEBUG);
            assertThat(mvcLogger.isDebugEnabled()).isTrue();

            mockMvc.perform(post(REGISTER_PATH)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson(
                                    EMAIL,
                                    REGISTER_LOG_PASSWORD,
                                    NICKNAME
                            )))
                    .andExpect(status().isCreated());

            assertThat(output.getAll())
                    .contains(
                            "password=REDACTED",
                            "accessToken=REDACTED"
                    )
                    .doesNotContain(
                            REGISTER_LOG_PASSWORD,
                            REGISTER_LOG_TOKEN
                    );
        } finally {
            mvcLogger.setLevel(originalLevel);
        }
        assertThat(mvcLogger.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void validLoginDoesNotWriteSecretsToDebugLogs(
            CapturedOutput output
    ) throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(successfulAuthentication(LOGIN_LOG_TOKEN));

        Logger mvcLogger = mvcAnnotationLogger();
        Level originalLevel = mvcLogger.getLevel();
        assertThat(originalLevel).isEqualTo(Level.INFO);
        try {
            mvcLogger.setLevel(Level.DEBUG);
            assertThat(mvcLogger.isDebugEnabled()).isTrue();

            mockMvc.perform(post(LOGIN_PATH)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(EMAIL, LOGIN_LOG_PASSWORD)))
                    .andExpect(status().isOk());

            assertThat(output.getAll())
                    .contains(
                            "password=REDACTED",
                            "accessToken=REDACTED"
                    )
                    .doesNotContain(
                            LOGIN_LOG_PASSWORD,
                            LOGIN_LOG_TOKEN
                    );
        } finally {
            mvcLogger.setLevel(originalLevel);
        }
        assertThat(mvcLogger.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void invalidPasswordValidationDoesNotWriteRejectedSecretToDebugLogs(
            CapturedOutput output
    ) throws Exception {
        assertThat(environment.getProperty(MVC_ANNOTATION_LOGGER_PROPERTY))
                .isEqualTo("INFO");
        assertThat(mvcAnnotationLogger().getLevel()).isEqualTo(Level.INFO);

        mockMvc.perform(post(REGISTER_PATH)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(
                                EMAIL,
                                VALIDATION_LOG_PASSWORD,
                                NICKNAME
                        )))
                .andExpect(status().isBadRequest());

        assertThat(output.getAll())
                .doesNotContain(VALIDATION_LOG_PASSWORD);
        assertThat(mvcAnnotationLogger().getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void registerRequestToStringRedactsPassword() {
        RegisterRequest request = new RegisterRequest(
                EMAIL,
                REGISTER_TO_STRING_PASSWORD,
                NICKNAME
        );

        assertThat(request.toString())
                .contains("password=REDACTED")
                .doesNotContain(REGISTER_TO_STRING_PASSWORD);
    }

    @Test
    void loginRequestToStringRedactsPassword() {
        LoginRequest request =
                new LoginRequest(EMAIL, LOGIN_TO_STRING_PASSWORD);

        assertThat(request.toString())
                .contains("password=REDACTED")
                .doesNotContain(LOGIN_TO_STRING_PASSWORD);
    }

    @Test
    void authResponseToStringRedactsAccessToken() {
        AuthResponse response =
                successfulAuthentication(AUTH_TO_STRING_TOKEN);

        assertThat(response.toString())
                .contains("accessToken=REDACTED")
                .doesNotContain(AUTH_TO_STRING_TOKEN);
    }

    private AuthResponse successfulAuthentication() {
        return successfulAuthentication(TEST_TOKEN);
    }

    private AuthResponse successfulAuthentication(String token) {
        return AuthResponse.bearer(
                token,
                7200,
                new CurrentUserResponse(
                        1L,
                        EMAIL,
                        NICKNAME,
                        UserRole.USER,
                        LocalDateTime.of(2026, 7, 28, 20, 15, 30)
                )
        );
    }

    private Logger mvcAnnotationLogger() {
        return (Logger) LoggerFactory.getLogger(MVC_ANNOTATION_LOGGER);
    }

    private String registerJson(
            String email,
            String password,
            String nickname
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "nickname", nickname
        ));
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
        ));
    }

    private String successfulAuthenticationJson(String message) {
        return """
                {
                  "code": "SUCCESS",
                  "message": "%s",
                  "data": {
                    "accessToken": "test.jwt.placeholder",
                    "tokenType": "Bearer",
                    "expiresIn": 7200,
                    "user": {
                      "id": 1,
                      "email": "student@example.com",
                      "nickname": "学习投资",
                      "role": "USER",
                      "createdAt": "2026-07-28T20:15:30"
                    }
                  }
                }
                """.formatted(message);
    }

    private String emailConflictJson() {
        return """
                {
                  "code": "USER_EMAIL_ALREADY_EXISTS",
                  "message": "该邮箱已注册",
                  "data": null
                }
                """;
    }

    private String validationFailureJson() {
        return """
                {
                  "code": "VALIDATION_FAILED",
                  "message": "请求参数不合法",
                  "data": null
                }
                """;
    }

    @RestController
    private static class DatabaseExceptionTestController {

        @PostMapping("/test/duplicate-key")
        void duplicateKey() {
            throw new DuplicateKeyException("test-only duplicate key");
        }

        @PostMapping("/test/nested-email-constraint")
        void nestedEmailConstraint() {
            SQLException constraintCause = new SQLException(
                    "Duplicate entry for key 'uk_sys_user_email'"
            );
            throw new DataIntegrityViolationException(
                    "test-only insert failure",
                    new IllegalStateException(
                            "test-only persistence wrapper",
                            constraintCause
                    )
            );
        }

        @PostMapping("/test/unrelated-integrity-constraint")
        void unrelatedIntegrityConstraint() {
            throw new DataIntegrityViolationException(
                    "test-only insert failure",
                    new SQLException(
                            "Duplicate entry for key 'uk_other_constraint'"
                    )
            );
        }
    }
}
