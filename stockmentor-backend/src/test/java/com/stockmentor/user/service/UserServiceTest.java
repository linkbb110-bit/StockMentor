package com.stockmentor.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.domain.UserRole;
import com.stockmentor.user.domain.UserStatus;
import com.stockmentor.user.dto.UpdateNicknameRequest;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import com.stockmentor.user.vo.CurrentUserResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 73L;
    private static final String EMAIL = "student@example.com";
    private static final String ORIGINAL_NICKNAME = "学习投资";
    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 7, 28, 20, 15, 30);

    @Mock
    private UserRepository userRepository;

    @Mock
    private NicknameNormalizer nicknameNormalizer;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, nicknameNormalizer);
    }

    @Test
    void getCurrentUserMapsOnlyTheSafeProfileForTheExactAuthenticatedUserId() {
        UserEntity user = profile(ORIGINAL_NICKNAME);
        when(userRepository.findIdentityById(USER_ID))
                .thenReturn(Optional.of(user));

        CurrentUserResponse response = service.getCurrentUser(USER_ID);

        verify(userRepository).findIdentityById(USER_ID);
        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.nickname()).isEqualTo(ORIGINAL_NICKNAME);
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        JsonNode responseJson = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .valueToTree(response);
        assertThat(responseJson.findValue("password")).isNull();
        assertThat(responseJson.findValue("passwordHash")).isNull();
        assertThat(responseJson.findValue("status")).isNull();
        assertThat(responseJson.findValue("deleted")).isNull();
    }

    @Test
    void getCurrentUserReturnsResourceNotFoundWhenTheExactUserIsMissing() {
        when(userRepository.findIdentityById(USER_ID))
                .thenReturn(Optional.empty());

        assertBusinessError(
                () -> service.getCurrentUser(USER_ID),
                ErrorCode.RESOURCE_NOT_FOUND
        );
    }

    @Test
    void updateNicknameNormalizesThenUpdatesAndReloadsTheExactCurrentUser() {
        String rawNickname = "  长期学习者  ";
        String normalizedNickname = "长期学习者";
        UserEntity latestUser = profile(normalizedNickname);
        when(nicknameNormalizer.normalize(rawNickname))
                .thenReturn(normalizedNickname);
        when(userRepository.updateNickname(USER_ID, normalizedNickname))
                .thenReturn(true);
        when(userRepository.findIdentityById(USER_ID))
                .thenReturn(Optional.of(latestUser));

        CurrentUserResponse response = service.updateNickname(
                USER_ID,
                new UpdateNicknameRequest(rawNickname)
        );

        InOrder order = inOrder(nicknameNormalizer, userRepository);
        order.verify(nicknameNormalizer).normalize(rawNickname);
        order.verify(userRepository)
                .updateNickname(USER_ID, normalizedNickname);
        order.verify(userRepository).findIdentityById(USER_ID);
        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.nickname()).isEqualTo(normalizedNickname);
    }

    @Test
    void updateNicknameReturnsResourceNotFoundWhenTheExactUpdateChangesNoUser() {
        String rawNickname = "  长期学习者  ";
        String normalizedNickname = "长期学习者";
        when(nicknameNormalizer.normalize(rawNickname))
                .thenReturn(normalizedNickname);
        when(userRepository.updateNickname(USER_ID, normalizedNickname))
                .thenReturn(false);

        assertBusinessError(
                () -> service.updateNickname(
                        USER_ID,
                        new UpdateNicknameRequest(rawNickname)
                ),
                ErrorCode.RESOURCE_NOT_FOUND
        );

        verify(userRepository)
                .updateNickname(USER_ID, normalizedNickname);
        verify(userRepository, never()).findIdentityById(anyLong());
    }

    @Test
    void updateNicknameReturnsResourceNotFoundWhenTheUpdatedUserCannotBeReloaded() {
        String normalizedNickname = "长期学习者";
        when(nicknameNormalizer.normalize(normalizedNickname))
                .thenReturn(normalizedNickname);
        when(userRepository.updateNickname(USER_ID, normalizedNickname))
                .thenReturn(true);
        when(userRepository.findIdentityById(USER_ID))
                .thenReturn(Optional.empty());

        assertBusinessError(
                () -> service.updateNickname(
                        USER_ID,
                        new UpdateNicknameRequest(normalizedNickname)
                ),
                ErrorCode.RESOURCE_NOT_FOUND
        );
    }

    @Test
    void updateNicknameRejectsPureSpacesBeforeAnyRepositoryAccess() {
        UserService serviceWithRealNormalizer =
                new UserService(userRepository, new NicknameNormalizer());

        assertBusinessError(
                () -> serviceWithRealNormalizer.updateNickname(
                        USER_ID,
                        new UpdateNicknameRequest("   ")
                ),
                ErrorCode.USER_NICKNAME_INVALID
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateNicknameRejectsIllegalCharactersBeforeAnyRepositoryAccess() {
        UserService serviceWithRealNormalizer =
                new UserService(userRepository, new NicknameNormalizer());

        assertBusinessError(
                () -> serviceWithRealNormalizer.updateNickname(
                        USER_ID,
                        new UpdateNicknameRequest("学习者!")
                ),
                ErrorCode.USER_NICKNAME_INVALID
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateNicknameDeclaresTheRequiredShortTransactionBoundary()
            throws NoSuchMethodException {
        Transactional transactional = UserService.class
                .getMethod(
                        "updateNickname",
                        long.class,
                        UpdateNicknameRequest.class
                )
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    private UserEntity profile(String nickname) {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$10$not-returned");
        user.setNickname(nickname);
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.of(2026, 7, 29, 9, 30));
        user.setCreatedAt(CREATED_AT);
        user.setUpdatedAt(LocalDateTime.of(2026, 7, 29, 9, 31));
        user.setDeleted(0);
        return user;
    }

    private void assertBusinessError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(expectedErrorCode);
                    assertThat(exception.getMessage())
                            .isEqualTo(expectedErrorCode.message());
                });
    }
}
