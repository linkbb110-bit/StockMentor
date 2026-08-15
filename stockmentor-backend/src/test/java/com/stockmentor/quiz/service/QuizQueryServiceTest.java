package com.stockmentor.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.repository.PublicQuizOptionRow;
import com.stockmentor.quiz.repository.PublicQuizQuestionRow;
import com.stockmentor.quiz.repository.PublishedQuizRow;
import com.stockmentor.quiz.repository.QuizRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizQueryServiceTest {

    @Mock
    private QuizRepository quizRepository;

    private QuizQueryService quizQueryService;

    @BeforeEach
    void setUp() {
        quizQueryService = new QuizQueryService(quizRepository);
    }

    @Test
    void assemblesOrderedQuestionsAndOptionsWithExactlyThreeRepositoryCalls() {
        PublishedQuizRow quiz = quiz();
        when(quizRepository.findPublishedByLessonId(101L)).thenReturn(Optional.of(quiz));
        when(quizRepository.findPublishedQuestions(7L)).thenReturn(List.of(
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
        ));
        when(quizRepository.findPublishedOptions(List.of(31L, 32L))).thenReturn(List.of(
                new PublicQuizOptionRow(301L, 31L, "A", "公司所有权的一部分", 1),
                new PublicQuizOptionRow(302L, 31L, "B", "固定收益承诺", 2),
                new PublicQuizOptionRow(303L, 32L, "TRUE", "正确", 1),
                new PublicQuizOptionRow(304L, 32L, "FALSE", "错误", 2)
        ));

        var response = quizQueryService.getPublishedQuiz(101L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.lessonId()).isEqualTo(101L);
        assertThat(response.courseTitle()).isEqualTo("股票投资基础");
        assertThat(response.questions()).extracting("questionId")
                .containsExactly(31L, 32L);
        assertThat(response.questions().get(0).options()).extracting("optionId")
                .containsExactly(301L, 302L);
        assertThat(response.questions().get(1).options()).extracting("optionId")
                .containsExactly(303L, 304L);
        verify(quizRepository).findPublishedByLessonId(101L);
        verify(quizRepository).findPublishedQuestions(7L);
        verify(quizRepository).findPublishedOptions(List.of(31L, 32L));
        verifyNoMoreInteractions(quizRepository);
    }

    @Test
    void unavailablePublishedChainReturnsTheSameResourceNotFound() {
        when(quizRepository.findPublishedByLessonId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizQueryService.getPublishedQuiz(404L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(quizRepository, never()).findPublishedQuestions(7L);
    }

    @Test
    void quizWithoutAnyPublishedQuestionIsUnavailable() {
        when(quizRepository.findPublishedByLessonId(101L)).thenReturn(Optional.of(quiz()));
        when(quizRepository.findPublishedQuestions(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> quizQueryService.getPublishedQuiz(101L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(quizRepository, never()).findPublishedOptions(org.mockito.ArgumentMatchers.anyList());
    }

    private PublishedQuizRow quiz() {
        return new PublishedQuizRow(
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
    }
}
