package com.stockmentor.quiz.vo;

import com.stockmentor.quiz.domain.WrongQuestionStatus;
import java.util.List;

public record WrongQuestionReviewResponse(
        long questionId,
        boolean correct,
        WrongQuestionStatus status,
        int errorCount,
        List<Long> correctOptionIds,
        String explanation
) {
}
