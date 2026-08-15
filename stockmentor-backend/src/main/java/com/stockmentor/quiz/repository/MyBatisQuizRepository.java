package com.stockmentor.quiz.repository;

import com.stockmentor.quiz.mapper.QuestionOptionMapper;
import com.stockmentor.quiz.mapper.QuizMapper;
import com.stockmentor.quiz.mapper.QuizQuestionMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisQuizRepository implements QuizRepository {

    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuestionOptionMapper questionOptionMapper;

    public MyBatisQuizRepository(
            QuizMapper quizMapper,
            QuizQuestionMapper quizQuestionMapper,
            QuestionOptionMapper questionOptionMapper
    ) {
        this.quizMapper = quizMapper;
        this.quizQuestionMapper = quizQuestionMapper;
        this.questionOptionMapper = questionOptionMapper;
    }

    @Override
    public Optional<PublishedQuizRow> findPublishedByLessonId(long lessonId) {
        return Optional.ofNullable(quizMapper.selectPublishedByLessonId(lessonId));
    }

    @Override
    public List<PublicQuizQuestionRow> findPublishedQuestions(long quizId) {
        return quizQuestionMapper.selectPublishedByQuizId(quizId);
    }

    @Override
    public List<PublicQuizOptionRow> findPublishedOptions(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        return questionOptionMapper.selectPublishedByQuestionIds(questionIds);
    }

    @Override
    public Optional<PublishedQuizRow> findPublishedByQuizId(long quizId) {
        return Optional.ofNullable(quizMapper.selectPublishedByQuizId(quizId));
    }

    @Override
    public List<QuizScoringQuestionRow> findScoringQuestions(long quizId) {
        return quizQuestionMapper.selectScoringByQuizId(quizId);
    }

    @Override
    public List<QuizScoringOptionRow> findScoringOptions(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        return questionOptionMapper.selectScoringByQuestionIds(questionIds);
    }

    @Override
    public Optional<QuizScoringQuestionRow> findScoringQuestion(long questionId) {
        return Optional.ofNullable(
                quizQuestionMapper.selectVisibleScoringByQuestionId(questionId)
        );
    }
}
