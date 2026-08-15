package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import com.stockmentor.quiz.mapper.WrongQuestionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisWrongQuestionRepository implements WrongQuestionRepository {

    private final WrongQuestionMapper mapper;

    public MyBatisWrongQuestionRepository(WrongQuestionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void recordWrong(long userId, long questionId, LocalDateTime reviewedAt) {
        mapper.upsertWrong(userId, questionId, reviewedAt);
    }

    @Override
    public void recordCorrect(long userId, long questionId, LocalDateTime reviewedAt) {
        mapper.updateCorrect(userId, questionId, reviewedAt);
    }

    @Override
    public Optional<WrongQuestionEntity> findState(long userId, long questionId) {
        return Optional.ofNullable(mapper.selectStateForUpdate(userId, questionId));
    }

    @Override
    public List<WrongQuestionRow> findVisibleByStatus(
            long userId,
            WrongQuestionStatus status
    ) {
        return mapper.selectVisibleByStatus(userId, status);
    }

    @Override
    public Optional<WrongQuestionRow> findVisibleByQuestion(
            long userId,
            long questionId
    ) {
        return Optional.ofNullable(mapper.selectVisibleByQuestion(userId, questionId));
    }
}
