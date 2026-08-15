package com.stockmentor.quiz.vo;

import com.stockmentor.quiz.domain.QuestionType;
import java.util.List;

public record QuizQuestionResponse(
        long questionId,
        QuestionType type,
        String stem,
        List<QuizOptionResponse> options
) {
}
