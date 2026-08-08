package com.stockmentor.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNicknameRequest(
        @NotNull String nickname
) {
}
