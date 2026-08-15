package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import java.time.LocalDateTime;

public record WrongQuestionRow(
        long id,
        long questionId,
        long lessonId,
        String lessonTitle,
        QuestionType type,
        String stem,
        WrongQuestionStatus status,
        int errorCount,
        LocalDateTime lastWrongAt,
        LocalDateTime masteredAt,
        LocalDateTime lastReviewedAt
) {
}
