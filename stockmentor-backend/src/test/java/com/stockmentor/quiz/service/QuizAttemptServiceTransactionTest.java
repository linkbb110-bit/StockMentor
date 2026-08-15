package com.stockmentor.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.dto.QuizAnswerRequest;
import com.stockmentor.quiz.dto.QuizAttemptRequest;
import com.stockmentor.quiz.repository.PublishedQuizRow;
import com.stockmentor.quiz.repository.QuizAttemptRepository;
import com.stockmentor.quiz.repository.QuizRepository;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import com.stockmentor.quiz.repository.WrongQuestionRepository;
import com.stockmentor.quiz.scoring.ObjectiveQuestionScorer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTransactionTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private WrongQuestionRepository wrongQuestionRepository;

    @Test
    void downstreamPersistenceFailurePropagatesAndMarksTheTransactionRollbackOnly() {
        when(quizRepository.findPublishedByQuizId(7L)).thenReturn(Optional.of(
                new PublishedQuizRow(
                        7L, 101L, "第一课", 1L, "股票投资基础",
                        11L, "第一章", "课后测验", "摘要"
                )
        ));
        when(quizRepository.findScoringQuestions(7L)).thenReturn(List.of(
                new QuizScoringQuestionRow(
                        1L, QuestionType.SINGLE_CHOICE, "解析", 1
                )
        ));
        when(quizRepository.findScoringOptions(List.of(1L))).thenReturn(List.of(
                new QuizScoringOptionRow(11L, 1L, true, 1)
        ));
        when(attemptRepository.createAttempt(any())).thenReturn(100L);
        when(attemptRepository.createAnswer(any())).thenReturn(1001L);
        doThrow(new IllegalStateException("forced answer-option failure"))
                .when(attemptRepository).createAnswerOptions(any());
        RecordingTransactionManager transactionManager =
                new RecordingTransactionManager();
        QuizAttemptService target = new QuizAttemptService(
                quizRepository,
                attemptRepository,
                wrongQuestionRepository,
                new ObjectiveQuestionScorer(),
                Clock.fixed(Instant.parse("2026-08-14T11:00:00Z"), ZoneOffset.UTC)
        );
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager,
                new AnnotationTransactionAttributeSource()
        ));
        QuizAttemptService transactionalService =
                (QuizAttemptService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.submit(
                42L,
                7L,
                new QuizAttemptRequest(List.of(
                        new QuizAnswerRequest(1L, List.of(11L))
                ))
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced answer-option failure");

        assertThat(transactionManager.begun).isTrue();
        assertThat(transactionManager.rolledBack).isTrue();
        assertThat(transactionManager.committed).isFalse();
    }

    private static final class RecordingTransactionManager
            extends AbstractPlatformTransactionManager {

        private boolean begun;
        private boolean committed;
        private boolean rolledBack;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            begun = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            committed = true;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rolledBack = true;
        }
    }
}
