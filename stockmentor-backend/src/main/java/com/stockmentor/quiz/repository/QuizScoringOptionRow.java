package com.stockmentor.quiz.repository;

public record QuizScoringOptionRow(
        long optionId,
        long questionId,
        boolean correct,
        int sortOrder
) {
}
