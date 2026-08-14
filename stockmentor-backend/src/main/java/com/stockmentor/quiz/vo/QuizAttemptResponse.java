package com.stockmentor.quiz.vo;

import java.util.List;

public record QuizAttemptResponse(
        long attemptId,
        int totalQuestions,
        int correctCount,
        int scorePercent,
        List<QuizQuestionResultResponse> results
) {
}
