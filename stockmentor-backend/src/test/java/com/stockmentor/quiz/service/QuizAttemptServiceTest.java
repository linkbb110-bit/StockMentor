package com.stockmentor.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.dto.QuizAnswerRequest;
import com.stockmentor.quiz.dto.QuizAttemptRequest;
import com.stockmentor.quiz.entity.QuizAnswerOptionEntity;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import com.stockmentor.quiz.repository.PublishedQuizRow;
import com.stockmentor.quiz.repository.QuizAttemptRepository;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import com.stockmentor.quiz.repository.WrongQuestionRepository;
import com.stockmentor.quiz.scoring.ObjectiveQuestionScorer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    private static final long USER_ID = 42L;
    private static final long QUIZ_ID = 7L;
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 14, 19, 0, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T11:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private WrongQuestionRepository wrongQuestionRepository;

    private QuizAttemptService service;

    @BeforeEach
    void setUp() {
        service = new QuizAttemptService(
                quizRepository,
                attemptRepository,
                wrongQuestionRepository,
                new ObjectiveQuestionScorer(),
                CLOCK
        );
    }

    @Test
    void validSubmissionPersistsNormalizedAnswersAndUsesFloorScoring() {
        stubPublishedSnapshot();
        when(attemptRepository.createAttempt(any())).thenReturn(100L);
        when(attemptRepository.createAnswer(any()))
                .thenReturn(1001L, 1002L, 1003L);

        var response = service.submit(USER_ID, QUIZ_ID, request(
                answer(3L, 31L),
                answer(1L, 12L),
                answer(2L, 23L, 21L)
        ));

        assertThat(response.attemptId()).isEqualTo(100L);
        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.correctCount()).isEqualTo(2);
        assertThat(response.scorePercent()).isEqualTo(66);
        assertThat(response.results()).extracting("questionId")
                .containsExactly(1L, 2L, 3L);
        assertThat(response.results().get(1).selectedOptionIds())
                .containsExactly(21L, 23L);
        assertThat(response.results().get(1).correctOptionIds())
                .containsExactly(21L, 23L);

        ArgumentCaptor<QuizAttemptEntity> attemptCaptor =
                ArgumentCaptor.forClass(QuizAttemptEntity.class);
        verify(attemptRepository).createAttempt(attemptCaptor.capture());
        QuizAttemptEntity attempt = attemptCaptor.getValue();
        assertThat(attempt.getUserId()).isEqualTo(USER_ID);
        assertThat(attempt.getQuizId()).isEqualTo(QUIZ_ID);
        assertThat(attempt.getTotalQuestions()).isEqualTo(3);
        assertThat(attempt.getCorrectCount()).isEqualTo(2);
        assertThat(attempt.getScorePercent()).isEqualTo(66);
        assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuizAnswerOptionEntity>> optionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(attemptRepository, times(3)).createAnswerOptions(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues().get(0))
                .extracting(QuizAnswerOptionEntity::getOptionId)
                .containsExactly(12L);
        assertThat(optionCaptor.getAllValues().get(1))
                .extracting(QuizAnswerOptionEntity::getOptionId)
                .containsExactly(21L, 23L);
        verify(wrongQuestionRepository).recordWrong(USER_ID, 1L, NOW);
        verify(wrongQuestionRepository).recordCorrect(USER_ID, 2L, NOW);
        verify(wrongQuestionRepository).recordCorrect(USER_ID, 3L, NOW);
    }

    @Test
    void allCorrectForcesOneHundredAndRepeatSubmissionCreatesAnotherAttempt() {
        stubPublishedSnapshot();
        when(attemptRepository.createAttempt(any())).thenReturn(100L, 101L);
        when(attemptRepository.createAnswer(any()))
                .thenReturn(1001L, 1002L, 1003L, 2001L, 2002L, 2003L);
        QuizAttemptRequest allCorrect = request(
                answer(1L, 11L),
                answer(2L, 21L, 23L),
                answer(3L, 31L)
        );

        var first = service.submit(USER_ID, QUIZ_ID, allCorrect);
        var second = service.submit(USER_ID, QUIZ_ID, allCorrect);

        assertThat(first.attemptId()).isEqualTo(100L);
        assertThat(second.attemptId()).isEqualTo(101L);
        assertThat(first.scorePercent()).isEqualTo(100);
        assertThat(second.scorePercent()).isEqualTo(100);
        verify(attemptRepository, times(2)).createAttempt(any());
    }

    @Test
    void unavailableOrZeroQuestionQuizReturns404BeforeAnyWrite() {
        when(quizRepository.findPublishedByQuizId(QUIZ_ID)).thenReturn(Optional.empty());

        assertResourceNotFound(() -> service.submit(
                USER_ID,
                QUIZ_ID,
                request(answer(1L, 11L))
        ));
        verifyNoInteractions(attemptRepository, wrongQuestionRepository);

        when(quizRepository.findPublishedByQuizId(QUIZ_ID))
                .thenReturn(Optional.of(quiz()));
        when(quizRepository.findScoringQuestions(QUIZ_ID)).thenReturn(List.of());

        assertResourceNotFound(() -> service.submit(
                USER_ID,
                QUIZ_ID,
                request(answer(1L, 11L))
        ));
        verify(attemptRepository, never()).createAttempt(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidCompleteSubmissionStopsBeforeAnyWrite(
            String description,
            QuizAttemptRequest invalidRequest
    ) {
        stubPublishedSnapshot();

        assertValidationFailure(() -> service.submit(USER_ID, QUIZ_ID, invalidRequest));

        verifyNoInteractions(attemptRepository, wrongQuestionRepository);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("missing question", request(
                        answer(1L, 11L), answer(2L, 21L, 23L)
                )),
                Arguments.of("extra question", request(
                        answer(1L, 11L), answer(2L, 21L, 23L),
                        answer(3L, 31L), answer(99L, 999L)
                )),
                Arguments.of("duplicate question", request(
                        answer(1L, 11L), answer(1L, 11L),
                        answer(2L, 21L, 23L), answer(3L, 31L)
                )),
                Arguments.of("foreign option", request(
                        answer(1L, 21L), answer(2L, 21L, 23L), answer(3L, 31L)
                )),
                Arguments.of("unknown option", request(
                        answer(1L, 999L), answer(2L, 21L, 23L), answer(3L, 31L)
                )),
                Arguments.of("invalid single cardinality", request(
                        answer(1L, 11L, 12L), answer(2L, 21L, 23L), answer(3L, 31L)
                )),
                Arguments.of("empty multiple", request(
                        answer(1L, 11L), answer(2L), answer(3L, 31L)
                )),
                Arguments.of("invalid true-false cardinality", request(
                        answer(1L, 11L), answer(2L, 21L, 23L), answer(3L, 31L, 32L)
                ))
        );
    }

    private void stubPublishedSnapshot() {
        when(quizRepository.findPublishedByQuizId(QUIZ_ID))
                .thenReturn(Optional.of(quiz()));
        when(quizRepository.findScoringQuestions(QUIZ_ID)).thenReturn(List.of(
                new QuizScoringQuestionRow(1L, QuestionType.SINGLE_CHOICE, "单选解析", 1),
                new QuizScoringQuestionRow(2L, QuestionType.MULTIPLE_CHOICE, "多选解析", 2),
                new QuizScoringQuestionRow(3L, QuestionType.TRUE_FALSE, "判断解析", 3)
        ));
        when(quizRepository.findScoringOptions(List.of(1L, 2L, 3L))).thenReturn(List.of(
                new QuizScoringOptionRow(11L, 1L, true, 1),
                new QuizScoringOptionRow(12L, 1L, false, 2),
                new QuizScoringOptionRow(21L, 2L, true, 1),
                new QuizScoringOptionRow(22L, 2L, false, 2),
                new QuizScoringOptionRow(23L, 2L, true, 3),
                new QuizScoringOptionRow(31L, 3L, true, 1),
                new QuizScoringOptionRow(32L, 3L, false, 2)
        ));
    }

    private PublishedQuizRow quiz() {
        return new PublishedQuizRow(
                QUIZ_ID, 101L, "第一课", 1L, "股票投资基础",
                11L, "第一章", "课后测验", "检验核心概念"
        );
    }

    private static QuizAttemptRequest request(QuizAnswerRequest... answers) {
        return new QuizAttemptRequest(List.of(answers));
    }

    private static QuizAnswerRequest answer(long questionId, Long... optionIds) {
        return new QuizAnswerRequest(questionId, List.of(optionIds));
    }

    private void assertValidationFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private void assertResourceNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
