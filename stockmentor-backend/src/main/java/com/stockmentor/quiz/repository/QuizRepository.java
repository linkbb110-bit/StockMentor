package com.stockmentor.quiz.repository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {

    Optional<PublishedQuizRow> findPublishedByLessonId(long lessonId);

    List<PublicQuizQuestionRow> findPublishedQuestions(long quizId);

    List<PublicQuizOptionRow> findPublishedOptions(List<Long> questionIds);

    Optional<PublishedQuizRow> findPublishedByQuizId(long quizId);

    List<QuizScoringQuestionRow> findScoringQuestions(long quizId);

    List<QuizScoringOptionRow> findScoringOptions(List<Long> questionIds);

    Optional<QuizScoringQuestionRow> findScoringQuestion(long questionId);
}
