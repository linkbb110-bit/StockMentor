package com.stockmentor.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull String email,
        @NotNull @Size(min = 8, max = 64) String password
) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=REDACTED]";
    }
}
