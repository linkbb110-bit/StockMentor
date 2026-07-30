package com.stockmentor.user.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.user.dto.UpdateNicknameRequest;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.repository.UserRepository;
import com.stockmentor.user.vo.CurrentUserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NicknameNormalizer nicknameNormalizer;

    public UserService(
            UserRepository userRepository,
            NicknameNormalizer nicknameNormalizer
    ) {
        this.userRepository = userRepository;
        this.nicknameNormalizer = nicknameNormalizer;
    }

    public CurrentUserResponse getCurrentUser(long userId) {
        UserEntity user = userRepository.findIdentityById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateNickname(
            long userId,
            UpdateNicknameRequest request
    ) {
        String normalizedNickname =
                nicknameNormalizer.normalize(request.nickname());
        if (!userRepository.updateNickname(userId, normalizedNickname)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return getCurrentUser(userId);
    }

    private CurrentUserResponse toResponse(UserEntity user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
