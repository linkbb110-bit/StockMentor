package com.stockmentor.quiz.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.mapper.QuestionOptionMapper;
import com.stockmentor.quiz.mapper.QuizMapper;
import com.stockmentor.quiz.mapper.QuizQuestionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBatisQuizRepositoryTest {

    @Mock
    private QuizMapper quizMapper;

    @Mock
    private QuizQuestionMapper quizQuestionMapper;

    @Mock
    private QuestionOptionMapper questionOptionMapper;

    private QuizRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisQuizRepository(
                quizMapper,
                quizQuestionMapper,
                questionOptionMapper
        );
    }

    @Test
    void delegatesThePublicReadToThreeFixedBulkQueries() {
        PublishedQuizRow quiz = new PublishedQuizRow(
                7L,
                101L,
                "股票是什么",
                1L,
                "股票投资基础",
                11L,
                "基础概念",
                "课后测验",
                "检验核心概念"
        );
        List<PublicQuizQuestionRow> questions = List.of(
                new PublicQuizQuestionRow(
                        31L,
                        QuestionType.SINGLE_CHOICE,
                        "股票通常代表什么？",
                        1
                ),
                new PublicQuizQuestionRow(
                        32L,
                        QuestionType.TRUE_FALSE,
                        "基金份额等于单一公司的所有权。",
                        2
                )
        );
        List<PublicQuizOptionRow> options = List.of(
                new PublicQuizOptionRow(301L, 31L, "A", "公司所有权的一部分", 1),
                new PublicQuizOptionRow(302L, 31L, "B", "固定收益承诺", 2),
                new PublicQuizOptionRow(303L, 32L, "TRUE", "正确", 1),
                new PublicQuizOptionRow(304L, 32L, "FALSE", "错误", 2)
        );
        when(quizMapper.selectPublishedByLessonId(101L)).thenReturn(quiz);
        when(quizQuestionMapper.selectPublishedByQuizId(7L)).thenReturn(questions);
        when(questionOptionMapper.selectPublishedByQuestionIds(List.of(31L, 32L)))
                .thenReturn(options);

        assertThat(repository.findPublishedByLessonId(101L)).contains(quiz);
        assertThat(repository.findPublishedQuestions(7L)).containsExactlyElementsOf(questions);
        assertThat(repository.findPublishedOptions(List.of(31L, 32L)))
                .containsExactlyElementsOf(options);

        verify(quizMapper).selectPublishedByLessonId(101L);
        verify(quizQuestionMapper).selectPublishedByQuizId(7L);
        verify(questionOptionMapper).selectPublishedByQuestionIds(List.of(31L, 32L));
        verifyNoMoreInteractions(quizMapper, quizQuestionMapper, questionOptionMapper);
    }

    @Test
    void missingOrUnpublishedQuizIsRepresentedAsEmpty() {
        when(quizMapper.selectPublishedByLessonId(404L)).thenReturn(null);

        assertThat(repository.findPublishedByLessonId(404L)).isEmpty();
    }
}
