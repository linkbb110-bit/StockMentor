package com.stockmentor.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmentor.auth.dto.LoginRequest;
import com.stockmentor.auth.dto.RegisterRequest;
import com.stockmentor.auth.vo.AuthResponse;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.infrastructure.config.SecurityBaselineConfig;
import com.stockmentor.infrastructure.security.JwtTokenProvider;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.domain.UserStatus;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import com.stockmentor.user.service.NicknameNormalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final long USER_ID = 41L;
    private static final long EXPIRATION_SECONDS = 7200L;
    private static final String PLAINTEXT_PASSWORD = "study123";
    private static final String TOKEN = "signed-jwt";
    private static final String NORMALIZED_EMAIL = "test@example.com";
    private static final String NORMALIZED_NICKNAME = "学习投资";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private PasswordEncoder passwordEncoder;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new AuthenticationService(
                userRepository,
                new EmailNormalizer(),
                new NicknameNormalizer(),
                passwordEncoder,
                jwtTokenProvider
        );
    }

    @Test
    void registerNormalizesIdentityAndPersistsOnlySafeAuthenticationDefaults() {
        LocalDateTime beforeRegistration = LocalDateTime.now();
        stubSuccessfulRegistration();

        service.register(new RegisterRequest(
                " Test@Example.com ",
                PLAINTEXT_PASSWORD,
                "  学习投资  "
        ));

        LocalDateTime afterRegistration = LocalDateTime.now();
        ArgumentCaptor<UserEntity> savedUserCaptor =
                ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<LocalDateTime> loginAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        InOrder order = inOrder(userRepository, jwtTokenProvider);
        order.verify(userRepository).findByNormalizedEmail(NORMALIZED_EMAIL);
        order.verify(userRepository).save(savedUserCaptor.capture());
        order.verify(userRepository)
                .updateLastLoginAt(eq(USER_ID), loginAtCaptor.capture());
        order.verify(jwtTokenProvider).issue(eq(USER_ID), any(Instant.class));

        UserEntity savedUser = savedUserCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(NORMALIZED_EMAIL);
        assertThat(savedUser.getNickname()).isEqualTo(NORMALIZED_NICKNAME);
        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(PLAINTEXT_PASSWORD)
                .startsWith("$2");
        assertThat(passwordEncoder.matches(
                PLAINTEXT_PASSWORD,
                savedUser.getPasswordHash()
        )).isTrue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getLastLoginAt())
                .isEqualTo(loginAtCaptor.getValue())
                .isBetween(beforeRegistration, afterRegistration);
        assertThat(savedUser.getCreatedAt())
                .isBetween(beforeRegistration, afterRegistration);
    }

    @Test
    void registerReturnsBearerResponseOnlyAfterIdAssignmentAndFirstLoginUpdate() {
        Instant beforeRegistration = Instant.now();
        stubSuccessfulRegistration();

        AuthResponse response = service.register(new RegisterRequest(
                " Test@Example.com ",
                PLAINTEXT_PASSWORD,
                NORMALIZED_NICKNAME
        ));

        Instant afterRegistration = Instant.now();
        ArgumentCaptor<Instant> issuedAtCaptor =
                ArgumentCaptor.forClass(Instant.class);
        InOrder order = inOrder(userRepository, jwtTokenProvider);
        order.verify(userRepository).findByNormalizedEmail(NORMALIZED_EMAIL);
        order.verify(userRepository).save(any(UserEntity.class));
        order.verify(userRepository)
                .updateLastLoginAt(eq(USER_ID), any(LocalDateTime.class));
        order.verify(jwtTokenProvider).issue(eq(USER_ID), issuedAtCaptor.capture());
        order.verify(jwtTokenProvider).expirationSeconds();

        assertThat(issuedAtCaptor.getValue())
                .isBetween(beforeRegistration, afterRegistration);
        assertThat(response.accessToken()).isEqualTo(TOKEN);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(EXPIRATION_SECONDS);
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().email()).isEqualTo(NORMALIZED_EMAIL);
        assertThat(response.user().nickname()).isEqualTo(NORMALIZED_NICKNAME);
        assertThat(response.user().role()).isEqualTo(UserRole.USER);
        assertThat(response.user().createdAt()).isNotNull();
    }

    @Test
    void registerResponseDoesNotExposePasswordOrInternalStatus() {
        stubSuccessfulRegistration();
        AuthResponse response = service.register(new RegisterRequest(
                NORMALIZED_EMAIL,
                PLAINTEXT_PASSWORD,
                NORMALIZED_NICKNAME
        ));
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        JsonNode responseJson = objectMapper.valueToTree(response);

        assertThat(responseJson.findValue("password")).isNull();
        assertThat(responseJson.findValue("passwordHash")).isNull();
        assertThat(responseJson.findValue("status")).isNull();
    }

    @Test
    void registerDeclaresTransactionalBoundary() throws NoSuchMethodException {
        Transactional transactional = AuthenticationService.class
                .getMethod("register", RegisterRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    @Test
    void securityConfigurationExposesBcryptPasswordEncoderBean() throws Exception {
        java.lang.reflect.Method passwordEncoderBean =
                java.util.Arrays.stream(SecurityBaselineConfig.class.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(Bean.class))
                        .filter(method -> method.getReturnType().equals(PasswordEncoder.class))
                        .findFirst()
                        .orElse(null);

        assertThat(passwordEncoderBean).isNotNull();
        passwordEncoderBean.setAccessible(true);
        assertThat(passwordEncoderBean.invoke(new SecurityBaselineConfig()))
                .isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void registerRejectsAnEmailAlreadyFoundBeforeInsert() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(activeUser()));

        assertBusinessError(
                () -> service.register(new RegisterRequest(
                        " Test@Example.com ",
                        PLAINTEXT_PASSWORD,
                        NORMALIZED_NICKNAME
                )),
                ErrorCode.USER_EMAIL_ALREADY_EXISTS
        );
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void registerMapsDuplicateKeyRaceToEmailAlreadyExistsWithoutIssuingToken() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenThrow(new DuplicateKeyException("uk_sys_user_email"));

        assertBusinessError(
                () -> service.register(new RegisterRequest(
                        NORMALIZED_EMAIL,
                        PLAINTEXT_PASSWORD,
                        NORMALIZED_NICKNAME
                )),
                ErrorCode.USER_EMAIL_ALREADY_EXISTS
        );
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void registerRequiresRepositoryAssignedIdBeforeUpdatingOrIssuingToken() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertBusinessError(
                () -> service.register(new RegisterRequest(
                        NORMALIZED_EMAIL,
                        PLAINTEXT_PASSWORD,
                        NORMALIZED_NICKNAME
                )),
                ErrorCode.INTERNAL_ERROR
        );
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void registerDoesNotIssueTokenWhenFirstLoginUpdateFails() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity savedUser = invocation.getArgument(0);
            assertThat(savedUser.getLastLoginAt()).isNull();
            savedUser.setId(USER_ID);
            return savedUser;
        });
        when(userRepository.updateLastLoginAt(
                eq(USER_ID),
                any(LocalDateTime.class)
        )).thenReturn(false);

        assertBusinessError(
                () -> service.register(new RegisterRequest(
                        NORMALIZED_EMAIL,
                        PLAINTEXT_PASSWORD,
                        NORMALIZED_NICKNAME
                )),
                ErrorCode.INTERNAL_ERROR
        );
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void loginAuthenticatesActiveUserThenUpdatesLastLoginBeforeIssuingToken() {
        UserEntity user = activeUser();
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(user));
        when(userRepository.updateLastLoginAt(
                eq(USER_ID),
                any(LocalDateTime.class)
        )).thenReturn(true);
        when(jwtTokenProvider.issue(eq(USER_ID), any(Instant.class)))
                .thenReturn(TOKEN);
        when(jwtTokenProvider.expirationSeconds())
                .thenReturn(EXPIRATION_SECONDS);
        LocalDateTime beforeLogin = LocalDateTime.now();

        AuthResponse response = service.login(new LoginRequest(
                " Test@Example.com ",
                PLAINTEXT_PASSWORD
        ));

        LocalDateTime afterLogin = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> loginAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        InOrder order = inOrder(userRepository, jwtTokenProvider);
        order.verify(userRepository).findByNormalizedEmail(NORMALIZED_EMAIL);
        order.verify(userRepository)
                .updateLastLoginAt(eq(USER_ID), loginAtCaptor.capture());
        order.verify(jwtTokenProvider).issue(eq(USER_ID), any(Instant.class));
        order.verify(jwtTokenProvider).expirationSeconds();

        assertThat(loginAtCaptor.getValue()).isBetween(beforeLogin, afterLogin);
        assertThat(user.getLastLoginAt()).isEqualTo(loginAtCaptor.getValue());
        assertThat(response.accessToken()).isEqualTo(TOKEN);
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().email()).isEqualTo(NORMALIZED_EMAIL);
        assertThat(response.user().nickname()).isEqualTo(NORMALIZED_NICKNAME);
        assertThat(response.user().role()).isEqualTo(UserRole.USER);
        assertThat(response.user().createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void wrongPasswordUsesTheUniformCredentialFailureAndDoesNotIssueToken() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(activeUser()));

        assertCredentialFailure(() -> service.login(new LoginRequest(
                NORMALIZED_EMAIL,
                "wrong123"
        )));
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void missingAccountUsesTheUniformCredentialFailureAndDoesNotIssueToken() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());

        assertCredentialFailure(() -> service.login(new LoginRequest(
                NORMALIZED_EMAIL,
                PLAINTEXT_PASSWORD
        )));
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void deletedAccountUnavailableFromRepositoryUsesTheUniformCredentialFailure() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());

        assertCredentialFailure(() -> service.login(new LoginRequest(
                NORMALIZED_EMAIL,
                PLAINTEXT_PASSWORD
        )));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void disabledAccountUsesTheUniformCredentialFailureAndDoesNotIssueToken() {
        UserEntity disabledUser = activeUser();
        disabledUser.setStatus(UserStatus.DISABLED);
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(disabledUser));

        assertCredentialFailure(() -> service.login(new LoginRequest(
                NORMALIZED_EMAIL,
                PLAINTEXT_PASSWORD
        )));
        verify(userRepository, never())
                .updateLastLoginAt(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void loginDoesNotIssueTokenWhenLastLoginUpdateFails() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(activeUser()));
        when(userRepository.updateLastLoginAt(
                eq(USER_ID),
                any(LocalDateTime.class)
        )).thenReturn(false);

        assertBusinessError(
                () -> service.login(new LoginRequest(
                        NORMALIZED_EMAIL,
                        PLAINTEXT_PASSWORD
                )),
                ErrorCode.INTERNAL_ERROR
        );
        verifyNoInteractions(jwtTokenProvider);
    }

    private void stubSuccessfulRegistration() {
        when(userRepository.findByNormalizedEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity savedUser = invocation.getArgument(0);
            savedUser.setId(USER_ID);
            return savedUser;
        });
        when(userRepository.updateLastLoginAt(
                eq(USER_ID),
                any(LocalDateTime.class)
        )).thenReturn(true);
        when(jwtTokenProvider.issue(eq(USER_ID), any(Instant.class)))
                .thenReturn(TOKEN);
        when(jwtTokenProvider.expirationSeconds())
                .thenReturn(EXPIRATION_SECONDS);
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(NORMALIZED_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PLAINTEXT_PASSWORD));
        user.setNickname(NORMALIZED_NICKNAME);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.of(2026, 7, 28, 20, 15, 30));
        user.setDeleted(0);
        return user;
    }

    private void assertCredentialFailure(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action
    ) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                    assertThat(exception.getMessage())
                            .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS.message());
                });
    }

    private void assertBusinessError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
                    assertThat(exception.getMessage()).isEqualTo(expectedErrorCode.message());
                });
    }
}
