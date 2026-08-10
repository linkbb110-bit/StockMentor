package com.stockmentor.course.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockmentor.course.entity.UserLessonProgressEntity;
import com.stockmentor.course.mapper.LearningProgressMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyBatisLearningProgressRepositoryTest {

    @Mock
    private LearningProgressMapper learningProgressMapper;

    private MyBatisLearningProgressRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyBatisLearningProgressRepository(learningProgressMapper);
    }

    @Test
    void zeroAffectedRowsFromDuplicateNoOpIsStillASuccessfulUpsert() {
        when(learningProgressMapper.upsertCompletion(42L, 101L)).thenReturn(0);

        assertThatCode(() -> repository.upsertCompletion(42L, 101L))
                .doesNotThrowAnyException();

        verify(learningProgressMapper).upsertCompletion(42L, 101L);
    }

    @Test
    void completionLookupAlwaysUsesBothCurrentUserAndLesson() {
        UserLessonProgressEntity progress = progress(42L, 101L);
        when(learningProgressMapper.selectCompletion(42L, 101L))
                .thenReturn(progress);

        assertThat(repository.findCompletion(42L, 101L)).containsSame(progress);

        verify(learningProgressMapper).selectCompletion(42L, 101L);
    }

    @Test
    void databaseProgressQueriesPreserveCurrentUserCourseAndStableRows() {
        NextLessonRow nextLesson = new NextLessonRow(
                102L,
                "指数如何描述一组资产",
                "理解指数的样本、权重与比较用途。",
                7,
                11L,
                "股票、基金、债券和指数"
        );
        when(learningProgressMapper.countPublishedLessons(7L)).thenReturn(3L);
        when(learningProgressMapper.countCompletedPublishedLessons(42L, 7L))
                .thenReturn(2L);
        when(learningProgressMapper.selectCompletedPublishedLessonIds(42L, 7L))
                .thenReturn(List.of(101L, 103L));
        when(learningProgressMapper.selectNextIncompletePublishedLesson(42L, 7L))
                .thenReturn(nextLesson);

        assertThat(repository.countPublishedLessons(7L)).isEqualTo(3L);
        assertThat(repository.countCompletedPublishedLessons(42L, 7L)).isEqualTo(2L);
        assertThat(repository.findCompletedPublishedLessonIds(42L, 7L))
                .containsExactly(101L, 103L);
        assertThat(repository.findNextIncompletePublishedLesson(42L, 7L))
                .containsSame(nextLesson);

        verify(learningProgressMapper).countPublishedLessons(7L);
        verify(learningProgressMapper).countCompletedPublishedLessons(42L, 7L);
        verify(learningProgressMapper).selectCompletedPublishedLessonIds(42L, 7L);
        verify(learningProgressMapper).selectNextIncompletePublishedLesson(42L, 7L);
    }

    private UserLessonProgressEntity progress(long userId, long lessonId) {
        UserLessonProgressEntity entity = new UserLessonProgressEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setLessonId(lessonId);
        entity.setCompletedAt(LocalDateTime.of(2026, 8, 10, 11, 0));
        entity.setCreatedAt(LocalDateTime.of(2026, 8, 10, 11, 0));
        return entity;
    }
}
