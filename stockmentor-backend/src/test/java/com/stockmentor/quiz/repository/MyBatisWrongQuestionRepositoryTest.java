package com.stockmentor.quiz.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import com.stockmentor.quiz.mapper.WrongQuestionMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBatisWrongQuestionRepositoryTest {

    private static final long USER_ID = 42L;
    private static final long QUESTION_ID = 11L;
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 14, 19, 30, 0);

    @Mock
    private WrongQuestionMapper mapper;

    private WrongQuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisWrongQuestionRepository(mapper);
    }

    @Test
    void wrongAndCorrectTransitionsAlwaysUseTheCurrentUserCompositeKey() {
        repository.recordWrong(USER_ID, QUESTION_ID, NOW);
        repository.recordCorrect(USER_ID, QUESTION_ID, NOW);

        verify(mapper).upsertWrong(USER_ID, QUESTION_ID, NOW);
        verify(mapper).updateCorrect(USER_ID, QUESTION_ID, NOW);
    }

    @Test
    void stateAndVisibleReadsRemainScopedToTheCurrentUser() {
        WrongQuestionEntity state = state(USER_ID, QUESTION_ID);
        WrongQuestionRow row = row(QUESTION_ID, WrongQuestionStatus.PENDING);
        when(mapper.selectStateForUpdate(USER_ID, QUESTION_ID)).thenReturn(state);
        when(mapper.selectVisibleByStatus(USER_ID, WrongQuestionStatus.PENDING))
                .thenReturn(List.of(row));
        when(mapper.selectVisibleByQuestion(USER_ID, QUESTION_ID)).thenReturn(row);

        assertThat(repository.findState(USER_ID, QUESTION_ID)).contains(state);
        assertThat(repository.findVisibleByStatus(USER_ID, WrongQuestionStatus.PENDING))
                .containsExactly(row);
        assertThat(repository.findVisibleByQuestion(USER_ID, QUESTION_ID)).contains(row);

        verify(mapper).selectStateForUpdate(USER_ID, QUESTION_ID);
        verify(mapper).selectVisibleByStatus(USER_ID, WrongQuestionStatus.PENDING);
        verify(mapper).selectVisibleByQuestion(USER_ID, QUESTION_ID);
    }

    private WrongQuestionEntity state(long userId, long questionId) {
        WrongQuestionEntity entity = new WrongQuestionEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setQuestionId(questionId);
        entity.setStatus(WrongQuestionStatus.PENDING);
        entity.setErrorCount(1);
        entity.setLastWrongAt(NOW);
        entity.setLastReviewedAt(NOW);
        return entity;
    }

    private WrongQuestionRow row(long questionId, WrongQuestionStatus status) {
        return new WrongQuestionRow(
                1L,
                questionId,
                101L,
                "第一课",
                QuestionType.SINGLE_CHOICE,
                "题干",
                status,
                1,
                NOW,
                null,
                NOW
        );
    }
}
