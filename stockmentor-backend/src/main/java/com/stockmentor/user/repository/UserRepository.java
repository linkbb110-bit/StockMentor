package com.stockmentor.user.repository;

import com.stockmentor.user.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository {

    Optional<UserEntity> findByNormalizedEmail(String normalizedEmail);

    Optional<UserEntity> findIdentityById(long userId);

    UserEntity save(UserEntity user);

    boolean updateLastLoginAt(long userId, LocalDateTime lastLoginAt);

    boolean updateNickname(long userId, String nickname);
}
