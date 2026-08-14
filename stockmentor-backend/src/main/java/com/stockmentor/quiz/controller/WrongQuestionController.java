package com.stockmentor.quiz.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.infrastructure.security.AuthenticatedUser;
import com.stockmentor.quiz.dto.WrongQuestionAnswerRequest;
import com.stockmentor.quiz.service.WrongQuestionService;
import com.stockmentor.quiz.vo.WrongQuestionResponse;
import com.stockmentor.quiz.vo.WrongQuestionReviewResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping
    public ApiResponse<List<WrongQuestionResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(wrongQuestionService.list(
                currentUser.userId(),
                status
        ));
    }

    @PostMapping("/{questionId}/answer")
    public ApiResponse<WrongQuestionReviewResponse> review(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable long questionId,
            @Valid @RequestBody WrongQuestionAnswerRequest request
    ) {
        return ApiResponse.success(wrongQuestionService.review(
                currentUser.userId(),
                questionId,
                request
        ));
    }
}
