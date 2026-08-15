package com.stockmentor.quiz.repository;

public record PublicQuizOptionRow(
        long optionId,
        long questionId,
        String optionKey,
        String content,
        int sortOrder
) {
}
