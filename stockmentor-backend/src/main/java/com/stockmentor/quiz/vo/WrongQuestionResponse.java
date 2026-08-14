package com.stockmentor.quiz.vo;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record WrongQuestionResponse(
        long questionId,
        long lessonId,
        String lessonTitle,
        QuestionType type,
        String stem,
        List<QuizOptionResponse> options,
        WrongQuestionStatus status,
        int errorCount,
        LocalDateTime lastWrongAt,
        LocalDateTime masteredAt
) {
}
