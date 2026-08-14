package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.domain.QuestionType;

public record QuizScoringQuestionRow(
        long questionId,
        QuestionType type,
        String explanation,
        int sortOrder
) {
}
