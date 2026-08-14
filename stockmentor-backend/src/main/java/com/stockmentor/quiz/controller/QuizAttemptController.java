package com.stockmentor.quiz.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.quiz.dto.QuizAttemptRequest;
import com.stockmentor.quiz.service.QuizAttemptService;
import com.stockmentor.quiz.vo.QuizAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/quizzes")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    public QuizAttemptController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @PostMapping("/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizAttemptResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable long quizId,
            @Valid @RequestBody QuizAttemptRequest request
    ) {
        return ApiResponse.success(quizAttemptService.submit(
                currentUser.userId(),
                quizId,
                request
        ));
    }
}
