package com.stockmentor.quiz.scoring;

import java.util.List;

public record ObjectiveScore(
        boolean correct,
        List<Long> selectedOptionIds,
        List<Long> correctOptionIds
) {
}
