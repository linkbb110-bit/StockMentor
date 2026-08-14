package com.stockmentor.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record QuizAttemptRequest(
        @NotEmpty List<@Valid QuizAnswerRequest> answers
) {
}
