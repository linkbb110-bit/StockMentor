package com.stockmentor.quiz.scoring;

public record ScoringOption(
        long optionId,
        boolean correct,
        int sortOrder
) {
}
