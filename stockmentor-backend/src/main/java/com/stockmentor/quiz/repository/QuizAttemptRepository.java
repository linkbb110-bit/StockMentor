package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.entity.QuizAnswerEntity;
import com.stockmentor.quiz.entity.QuizAnswerOptionEntity;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import java.util.List;

public interface QuizAttemptRepository {

    long createAttempt(QuizAttemptEntity attempt);

    long createAnswer(QuizAnswerEntity answer);

    void createAnswerOptions(List<QuizAnswerOptionEntity> selectedOptions);
}
