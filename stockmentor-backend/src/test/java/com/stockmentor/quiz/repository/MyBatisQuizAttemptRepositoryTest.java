package com.stockmentor.quiz.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockmentor.quiz.entity.QuizAnswerEntity;
import com.stockmentor.quiz.entity.QuizAnswerOptionEntity;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import com.stockmentor.quiz.mapper.QuizAnswerMapper;
import com.stockmentor.quiz.mapper.QuizAnswerOptionMapper;
import com.stockmentor.quiz.mapper.QuizAttemptMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBatisQuizAttemptRepositoryTest {

    @Mock
    private QuizAttemptMapper attemptMapper;

    @Mock
    private QuizAnswerMapper answerMapper;

    @Mock
    private QuizAnswerOptionMapper answerOptionMapper;

    private QuizAttemptRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisQuizAttemptRepository(
                attemptMapper,
                answerMapper,
                answerOptionMapper
        );
    }

    @Test
    void insertsAttemptAnswerAndEveryNormalizedSelectedOption() {
        QuizAttemptEntity attempt = new QuizAttemptEntity();
        when(attemptMapper.insert(attempt)).thenAnswer(invocation -> {
            attempt.setId(100L);
            return 1;
        });
        QuizAnswerEntity answer = new QuizAnswerEntity();
        when(answerMapper.insert(answer)).thenAnswer(invocation -> {
            answer.setId(1001L);
            return 1;
        });
        List<QuizAnswerOptionEntity> selections = List.of(
                selection(1001L, 21L),
                selection(1001L, 23L)
        );

        assertThat(repository.createAttempt(attempt)).isEqualTo(100L);
        assertThat(repository.createAnswer(answer)).isEqualTo(1001L);
        repository.createAnswerOptions(selections);

        verify(attemptMapper).insert(attempt);
        verify(answerMapper).insert(answer);
        verify(answerOptionMapper).insertSelection(1001L, 21L);
        verify(answerOptionMapper).insertSelection(1001L, 23L);
    }

    private QuizAnswerOptionEntity selection(long answerId, long optionId) {
        QuizAnswerOptionEntity entity = new QuizAnswerOptionEntity();
        entity.setQuizAnswerId(answerId);
        entity.setOptionId(optionId);
        return entity;
    }
}
