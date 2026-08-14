package com.stockmentor.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.dto.WrongQuestionAnswerRequest;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import com.stockmentor.quiz.repository.PublicQuizOptionRow;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import com.stockmentor.quiz.repository.WrongQuestionRepository;
import com.stockmentor.quiz.repository.WrongQuestionRow;
import com.stockmentor.quiz.scoring.ObjectiveQuestionScorer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrongQuestionServiceTest {

    private static final long USER_A = 42L;
    private static final long USER_B = 84L;
    private static final long QUESTION_ID = 11L;
    private static final LocalDateTime OLD_WRONG_AT =
            LocalDateTime.of(2026, 8, 13, 9, 0, 0);
    private static final LocalDateTime OLD_MASTERED_AT =
            LocalDateTime.of(2026, 8, 13, 10, 0, 0);
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 14, 19, 45, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T11:45:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private WrongQuestionRepository wrongQuestionRepository;

    @Mock
    private QuizRepository quizRepository;

    private WrongQuestionService service;

    @BeforeEach
    void setUp() {
        service = new WrongQuestionService(
                wrongQuestionRepository,
                quizRepository,
                new ObjectiveQuestionScorer(),
                CLOCK
        );
    }

    @Test
    void nullStatusDefaultsToPendingAndOptionsAreLoadedInOneBatch() {
        WrongQuestionRow newest = row(12L, WrongQuestionStatus.PENDING, 3);
        WrongQuestionRow older = row(11L, WrongQuestionStatus.PENDING, 1);
        when(wrongQuestionRepository.findVisibleByStatus(
                USER_A,
                WrongQuestionStatus.PENDING
        )).thenReturn(List.of(newest, older));
        when(quizRepository.findPublishedOptions(List.of(12L, 11L))).thenReturn(List.of(
                new PublicQuizOptionRow(121L, 12L, "A", "选项 A", 1),
                new PublicQuizOptionRow(122L, 12L, "B", "选项 B", 2),
                new PublicQuizOptionRow(111L, 11L, "TRUE", "正确", 1),
                new PublicQuizOptionRow(112L, 11L, "FALSE", "错误", 2)
        ));

        var result = service.list(USER_A, null);

        assertThat(result).extracting("questionId").containsExactly(12L, 11L);
        assertThat(result.get(0).options()).extracting("optionId")
                .containsExactly(121L, 122L);
        assertThat(result.get(1).options()).extracting("optionId")
                .containsExactly(111L, 112L);
        assertThat(result.get(0).errorCount()).isEqualTo(3);
        verify(quizRepository).findPublishedOptions(List.of(12L, 11L));
    }

    @ParameterizedTest
    @EnumSource(WrongQuestionStatus.class)
    void explicitStatusIsPassedToTheUserScopedQuery(WrongQuestionStatus status) {
        when(wrongQuestionRepository.findVisibleByStatus(
                USER_A,
                status
        )).thenReturn(List.of());

        assertThat(service.list(USER_A, status.name())).isEmpty();

        verify(wrongQuestionRepository).findVisibleByStatus(
                USER_A,
                status
        );
        verify(quizRepository, never()).findPublishedOptions(List.of());
    }

    @Test
    void invalidStatusReturnsValidationFailureBeforeAnyQuery() {
        assertValidationFailure(() -> service.list(USER_A, "ARCHIVED"));

        verifyNoInteractions(wrongQuestionRepository, quizRepository);
    }

    @Test
    void pendingCorrectBecomesMasteredWithThePersistedState() {
        stubVisibleReview(WrongQuestionStatus.PENDING);
        stubScoringQuestion();
        WrongQuestionEntity mastered = state(
                WrongQuestionStatus.MASTERED,
                2,
                OLD_WRONG_AT,
                NOW
        );
        when(wrongQuestionRepository.findState(USER_A, QUESTION_ID))
                .thenReturn(Optional.of(mastered));

        var result = service.review(
                USER_A,
                QUESTION_ID,
                new WrongQuestionAnswerRequest(List.of(111L))
        );

        assertThat(result.correct()).isTrue();
        assertThat(result.status()).isEqualTo(WrongQuestionStatus.MASTERED);
        assertThat(result.errorCount()).isEqualTo(2);
        assertThat(result.correctOptionIds()).containsExactly(111L);
        assertThat(result.explanation()).isEqualTo("判断题解析");
        verify(wrongQuestionRepository).recordCorrect(USER_A, QUESTION_ID, NOW);
    }

    @Test
    void pendingWrongStaysPendingAndIncrementsThroughRepositoryState() {
        stubVisibleReview(WrongQuestionStatus.PENDING);
        stubScoringQuestion();
        when(wrongQuestionRepository.findState(USER_A, QUESTION_ID))
                .thenReturn(Optional.of(state(
                        WrongQuestionStatus.PENDING,
                        3,
                        NOW,
                        null
                )));

        var result = service.review(
                USER_A,
                QUESTION_ID,
                new WrongQuestionAnswerRequest(List.of(112L))
        );

        assertThat(result.correct()).isFalse();
        assertThat(result.status()).isEqualTo(WrongQuestionStatus.PENDING);
        assertThat(result.errorCount()).isEqualTo(3);
        verify(wrongQuestionRepository).recordWrong(USER_A, QUESTION_ID, NOW);
    }

    @Test
    void masteredWrongReturnsToPendingAndIncrementsErrorCount() {
        stubVisibleReview(WrongQuestionStatus.MASTERED);
        stubScoringQuestion();
        when(wrongQuestionRepository.findState(USER_A, QUESTION_ID))
                .thenReturn(Optional.of(state(
                        WrongQuestionStatus.PENDING,
                        3,
                        NOW,
                        null
                )));

        var result = service.review(
                USER_A,
                QUESTION_ID,
                new WrongQuestionAnswerRequest(List.of(112L))
        );

        assertThat(result.status()).isEqualTo(WrongQuestionStatus.PENDING);
        assertThat(result.errorCount()).isEqualTo(3);
        verify(wrongQuestionRepository).recordWrong(USER_A, QUESTION_ID, NOW);
    }

    @Test
    void masteredCorrectPreservesTheOriginalMasteredTimestamp() {
        stubVisibleReview(WrongQuestionStatus.MASTERED);
        stubScoringQuestion();
        WrongQuestionEntity mastered = state(
                WrongQuestionStatus.MASTERED,
                2,
                OLD_WRONG_AT,
                OLD_MASTERED_AT
        );
        when(wrongQuestionRepository.findState(USER_A, QUESTION_ID))
                .thenReturn(Optional.of(mastered));

        var result = service.review(
                USER_A,
                QUESTION_ID,
                new WrongQuestionAnswerRequest(List.of(111L))
        );

        assertThat(result.status()).isEqualTo(WrongQuestionStatus.MASTERED);
        assertThat(mastered.getMasteredAt()).isEqualTo(OLD_MASTERED_AT);
        assertThat(mastered.getLastWrongAt()).isEqualTo(OLD_WRONG_AT);
        assertThat(mastered.getLastReviewedAt()).isEqualTo(NOW);
        verify(wrongQuestionRepository).recordCorrect(USER_A, QUESTION_ID, NOW);
    }

    @Test
    void anotherUserCannotReviewOrMutateAnOwnedWrongQuestion() {
        when(wrongQuestionRepository.findVisibleByQuestion(USER_B, QUESTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(
                USER_B,
                QUESTION_ID,
                new WrongQuestionAnswerRequest(List.of(111L))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(wrongQuestionRepository, never())
                .recordCorrect(USER_A, QUESTION_ID, NOW);
        verify(wrongQuestionRepository, never())
                .recordWrong(USER_A, QUESTION_ID, NOW);
        verifyNoInteractions(quizRepository);
    }

    private void stubVisibleReview(WrongQuestionStatus status) {
        when(wrongQuestionRepository.findVisibleByQuestion(USER_A, QUESTION_ID))
                .thenReturn(Optional.of(row(QUESTION_ID, status, 2)));
    }

    private void stubScoringQuestion() {
        when(quizRepository.findScoringQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuizScoringQuestionRow(
                        QUESTION_ID,
                        QuestionType.TRUE_FALSE,
                        "判断题解析",
                        1
                )
        ));
        when(quizRepository.findScoringOptions(List.of(QUESTION_ID))).thenReturn(List.of(
                new QuizScoringOptionRow(111L, QUESTION_ID, true, 1),
                new QuizScoringOptionRow(112L, QUESTION_ID, false, 2)
        ));
    }

    private WrongQuestionRow row(
            long questionId,
            WrongQuestionStatus status,
            int errorCount
    ) {
        return new WrongQuestionRow(
                questionId,
                questionId,
                101L,
                "第一课",
                QuestionType.TRUE_FALSE,
                "判断题题干",
                status,
                errorCount,
                OLD_WRONG_AT,
                status == WrongQuestionStatus.MASTERED ? OLD_MASTERED_AT : null,
                OLD_WRONG_AT
        );
    }

    private WrongQuestionEntity state(
            WrongQuestionStatus status,
            int errorCount,
            LocalDateTime lastWrongAt,
            LocalDateTime masteredAt
    ) {
        WrongQuestionEntity entity = new WrongQuestionEntity();
        entity.setId(1L);
        entity.setUserId(USER_A);
        entity.setQuestionId(QUESTION_ID);
        entity.setStatus(status);
        entity.setErrorCount(errorCount);
        entity.setLastWrongAt(lastWrongAt);
        entity.setMasteredAt(masteredAt);
        entity.setLastReviewedAt(NOW);
        return entity;
    }

    private void assertValidationFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
