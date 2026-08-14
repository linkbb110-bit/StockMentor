package com.stockmentor.quiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record QuizAnswerRequest(
        @Positive long questionId,
        @NotNull List<@NotNull @Positive Long> selectedOptionIds
) {
}
