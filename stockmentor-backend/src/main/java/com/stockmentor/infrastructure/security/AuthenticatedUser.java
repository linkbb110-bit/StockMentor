package com.stockmentor.infrastructure.security;

import com.stockmentor.user.domain.UserRole;

public record AuthenticatedUser(
        long userId,
        String email,
        UserRole role
) {
}
