package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.entity.QuizAnswerEntity;
import com.stockmentor.quiz.entity.QuizAnswerOptionEntity;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import com.stockmentor.quiz.mapper.QuizAnswerMapper;
import com.stockmentor.quiz.mapper.QuizAnswerOptionMapper;
import com.stockmentor.quiz.mapper.QuizAttemptMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisQuizAttemptRepository implements QuizAttemptRepository {

    private final QuizAttemptMapper attemptMapper;
    private final QuizAnswerMapper answerMapper;
    private final QuizAnswerOptionMapper answerOptionMapper;

    public MyBatisQuizAttemptRepository(
            QuizAttemptMapper attemptMapper,
            QuizAnswerMapper answerMapper,
            QuizAnswerOptionMapper answerOptionMapper
    ) {
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
        this.answerOptionMapper = answerOptionMapper;
    }

    @Override
    public long createAttempt(QuizAttemptEntity attempt) {
        attemptMapper.insert(attempt);
        return requiredId(attempt.getId());
    }

    @Override
    public long createAnswer(QuizAnswerEntity answer) {
        answerMapper.insert(answer);
        return requiredId(answer.getId());
    }

    @Override
    public void createAnswerOptions(List<QuizAnswerOptionEntity> selectedOptions) {
        for (QuizAnswerOptionEntity selectedOption : selectedOptions) {
            answerOptionMapper.insertSelection(
                    selectedOption.getQuizAnswerId(),
                    selectedOption.getOptionId()
            );
        }
    }

    private long requiredId(Long id) {
        if (id == null) {
            throw new IllegalStateException("Generated persistence ID is missing");
        }
        return id;
    }
}
