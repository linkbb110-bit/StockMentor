package com.stockmentor.quiz.controller;

import com.stockmentor.common.api.ApiResponse;
import com.stockmentor.quiz.service.QuizQueryService;
import com.stockmentor.quiz.vo.PublicQuizResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lessons")
public class QuizController {

    private final QuizQueryService quizQueryService;

    public QuizController(QuizQueryService quizQueryService) {
        this.quizQueryService = quizQueryService;
    }

    @GetMapping("/{lessonId}/quiz")
    public ApiResponse<PublicQuizResponse> getQuiz(@PathVariable long lessonId) {
        return ApiResponse.success(quizQueryService.getPublishedQuiz(lessonId));
    }
}
