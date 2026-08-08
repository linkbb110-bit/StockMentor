package com.stockmentor.auth.dto;

import com.stockmentor.auth.validation.BcryptCompatiblePassword;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull String email,
        @NotNull
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$")
        @BcryptCompatiblePassword
        String password,
        @NotNull String nickname
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email
                + ", password=REDACTED, nickname=" + nickname + "]";
    }
}
