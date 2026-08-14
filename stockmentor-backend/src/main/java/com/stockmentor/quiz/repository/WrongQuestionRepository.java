package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository {

    void recordWrong(long userId, long questionId, LocalDateTime reviewedAt);

    void recordCorrect(long userId, long questionId, LocalDateTime reviewedAt);

    Optional<WrongQuestionEntity> findState(long userId, long questionId);

    List<WrongQuestionRow> findVisibleByStatus(
            long userId,
            WrongQuestionStatus status
    );

    Optional<WrongQuestionRow> findVisibleByQuestion(long userId, long questionId);
}
