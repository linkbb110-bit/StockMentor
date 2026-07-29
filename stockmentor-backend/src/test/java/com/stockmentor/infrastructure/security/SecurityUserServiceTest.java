package com.stockmentor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.domain.UserStatus;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityUserServiceTest {

    private static final long USER_ID = 42L;
    private static final String EMAIL = "latest@example.com";

    @Mock
    private UserRepository userRepository;

    private SecurityUserService service;

    @BeforeEach
    void setUp() {
        service = new SecurityUserService(userRepository);
    }

    @Test
    void activeUserMapsTheLatestDatabaseIdentity() {
        UserEntity user = identity(UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findIdentityById(USER_ID)).thenReturn(Optional.of(user));

        AuthenticatedUser authenticatedUser = service.load(USER_ID);

        assertThat(authenticatedUser.userId()).isEqualTo(USER_ID);
        assertThat(authenticatedUser.email()).isEqualTo(EMAIL);
        assertThat(authenticatedUser.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void disabledUserUsesTheInternalDisabledClassification() {
        when(userRepository.findIdentityById(USER_ID))
                .thenReturn(Optional.of(identity(UserRole.USER, UserStatus.DISABLED)));

        assertBusinessError(() -> service.load(USER_ID), ErrorCode.AUTH_USER_DISABLED);
    }

    @Test
    void missingOrLogicallyDeletedUserUsesTheInternalNotFoundClassification() {
        when(userRepository.findIdentityById(USER_ID)).thenReturn(Optional.empty());

        assertBusinessError(() -> service.load(USER_ID), ErrorCode.AUTH_USER_NOT_FOUND);
    }

    private UserEntity identity(UserRole role, UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private void assertBusinessError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
