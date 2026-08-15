package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.domain.QuestionType;

public record PublicQuizQuestionRow(
        long questionId,
        QuestionType type,
        String stem,
        int sortOrder
) {
}
