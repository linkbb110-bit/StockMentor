package com.stockmentor.quiz.repository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository {

    Optional<PublishedQuizRow> findPublishedByLessonId(long lessonId);

    List<PublicQuizQuestionRow> findPublishedQuestions(long quizId);

    List<PublicQuizOptionRow> findPublishedOptions(List<Long> questionIds);
}
