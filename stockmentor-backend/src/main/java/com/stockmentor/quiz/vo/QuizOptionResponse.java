package com.stockmentor.quiz.vo;

public record QuizOptionResponse(
        long optionId,
        String optionKey,
        String content
) {
}
