package com.stockmentor.infrastructure.security;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.domain.UserStatus;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserService {
    private final UserRepository userRepository;

    public SecurityUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUser load(long userId) {
        UserEntity user = userRepository.findIdentityById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
        }

        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
    }
}
