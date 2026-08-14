package com.stockmentor.quiz.vo;

import java.util.List;

public record QuizQuestionResultResponse(
        long questionId,
        boolean correct,
        List<Long> selectedOptionIds,
        List<Long> correctOptionIds,
        String explanation
) {
}
