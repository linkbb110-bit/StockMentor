package com.stockmentor.user.vo;

import com.stockmentor.user.domain.UserRole;
import java.time.LocalDateTime;

public record CurrentUserResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        LocalDateTime createdAt
) {
}
