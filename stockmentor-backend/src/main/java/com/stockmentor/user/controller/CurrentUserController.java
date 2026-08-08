package com.stockmentor.user.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.user.dto.UpdateNicknameRequest;
import com.stockmentor.user.service.UserService;
import com.stockmentor.user.vo.CurrentUserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {

    private final UserService userService;

    public CurrentUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                userService.getCurrentUser(currentUser.userId())
        );
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<CurrentUserResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ApiResponse.success(
                userService.updateNickname(currentUser.userId(), request)
        );
    }
}
