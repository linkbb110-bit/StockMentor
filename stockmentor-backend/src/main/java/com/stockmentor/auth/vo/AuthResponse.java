package com.stockmentor.auth.vo;

import com.stockmentor.user.vo.CurrentUserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        CurrentUserResponse user
) {
    public static AuthResponse bearer(
            String token,
            long expiresIn,
            CurrentUserResponse user
    ) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }

    @Override
    public String toString() {
        return "AuthResponse[accessToken=REDACTED, tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }
}
