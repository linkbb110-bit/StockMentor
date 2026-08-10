package com.stockmentor.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.UserLessonProgressEntity;
import com.stockmentor.course.repository.CourseRepository;
import com.stockmentor.course.repository.LearningProgressRepository;
import com.stockmentor.course.repository.NextLessonRow;
import com.stockmentor.course.repository.PublishedLessonRow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceTest {

    private static final long COURSE_ID = 7L;
    private static final long USER_A = 42L;
    private static final long USER_B = 84L;
    private static final long LESSON_ID = 101L;
    private static final LocalDateTime FIRST_COMPLETED_AT =
            LocalDateTime.of(2026, 8, 10, 11, 5, 30);

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LearningProgressRepository learningProgressRepository;

    private LearningProgressService service;

    @BeforeEach
    void setUp() {
        service = new LearningProgressService(courseRepository, learningProgressRepository);
    }

    @Test
    void missingOrUnpublishedLessonStopsBeforeAnyProgressWrite() {
        when(courseRepository.findPublishedLessonById(LESSON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeLesson(USER_A, LESSON_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(courseRepository).findPublishedLessonById(LESSON_ID);
        verifyNoInteractions(learningProgressRepository);
    }

    @Test
    void firstAndRepeatedCompletionReturnThePersistedFirstTimestamp() {
        UserLessonProgressEntity persisted = progress(USER_A, LESSON_ID, FIRST_COMPLETED_AT);
        when(courseRepository.findPublishedLessonById(LESSON_ID))
                .thenReturn(Optional.of(publishedLesson(LESSON_ID)));
        when(learningProgressRepository.findCompletion(USER_A, LESSON_ID))
                .thenReturn(Optional.of(persisted));

        var first = service.completeLesson(USER_A, LESSON_ID);
        var repeated = service.completeLesson(USER_A, LESSON_ID);

        assertThat(first.lessonId()).isEqualTo(LESSON_ID);
        assertThat(first.completed()).isTrue();
        assertThat(first.completedAt()).isEqualTo(FIRST_COMPLETED_AT);
        assertThat(repeated).isEqualTo(first);
        verify(learningProgressRepository, times(2))
                .upsertCompletion(USER_A, LESSON_ID);
        verify(learningProgressRepository, times(2))
                .findCompletion(USER_A, LESSON_ID);
    }

    @Test
    void unpublishedOrMissingCourseStopsBeforeProgressQueries() {
        when(courseRepository.findPublishedCourseById(COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCourseProgress(USER_A, COURSE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(courseRepository).findPublishedCourseById(COURSE_ID);
        verifyNoInteractions(learningProgressRepository);
    }

    @Test
    void courseWithNoPublishedLessonsReturnsZeroAndNoNextLesson() {
        stubPublishedCourse();
        stubProgress(USER_A, 0L, 0L, List.of(), Optional.empty());

        var result = service.getCourseProgress(USER_A, COURSE_ID);

        assertThat(result.completedLessons()).isZero();
        assertThat(result.totalLessons()).isZero();
        assertThat(result.progressPercent()).isZero();
        assertThat(result.completedLessonIds()).isEmpty();
        assertThat(result.nextLesson()).isNull();
    }

    @Test
    void usersStayIsolatedAndSkippingALessonKeepsItAsNext() {
        stubPublishedCourse();
        NextLessonRow userANext = nextLesson(102L, "第二课");
        NextLessonRow userBNext = nextLesson(101L, "第一课");
        stubProgress(
                USER_A,
                3L,
                2L,
                List.of(101L, 103L),
                Optional.of(userANext)
        );
        stubProgress(
                USER_B,
                3L,
                0L,
                List.of(),
                Optional.of(userBNext)
        );

        var userA = service.getCourseProgress(USER_A, COURSE_ID);
        var userB = service.getCourseProgress(USER_B, COURSE_ID);

        assertThat(userA.completedLessons()).isEqualTo(2L);
        assertThat(userA.progressPercent()).isEqualTo(66);
        assertThat(userA.completedLessonIds()).containsExactly(101L, 103L);
        assertThat(userA.nextLesson().id()).isEqualTo(102L);
        assertThat(userB.completedLessons()).isZero();
        assertThat(userB.progressPercent()).isZero();
        assertThat(userB.completedLessonIds()).isEmpty();
        assertThat(userB.nextLesson().id()).isEqualTo(101L);
    }

    @Test
    void allPublishedLessonsCompleteReturnsOneHundredAndNullNextLesson() {
        stubPublishedCourse();
        stubProgress(
                USER_A,
                3L,
                3L,
                List.of(101L, 102L, 103L),
                Optional.empty()
        );

        var result = service.getCourseProgress(USER_A, COURSE_ID);

        assertThat(result.completedLessons()).isEqualTo(3L);
        assertThat(result.totalLessons()).isEqualTo(3L);
        assertThat(result.progressPercent()).isEqualTo(100);
        assertThat(result.completedLessonIds()).containsExactly(101L, 102L, 103L);
        assertThat(result.nextLesson()).isNull();
    }

    private void stubPublishedCourse() {
        CourseEntity course = new CourseEntity();
        course.setId(COURSE_ID);
        course.setPublished(true);
        when(courseRepository.findPublishedCourseById(COURSE_ID))
                .thenReturn(Optional.of(course));
    }

    private void stubProgress(
            long userId,
            long total,
            long completed,
            List<Long> completedIds,
            Optional<NextLessonRow> nextLesson
    ) {
        when(learningProgressRepository.countPublishedLessons(COURSE_ID))
                .thenReturn(total);
        when(learningProgressRepository.countCompletedPublishedLessons(
                userId,
                COURSE_ID
        )).thenReturn(completed);
        when(learningProgressRepository.findCompletedPublishedLessonIds(
                userId,
                COURSE_ID
        )).thenReturn(completedIds);
        when(learningProgressRepository.findNextIncompletePublishedLesson(
                userId,
                COURSE_ID
        )).thenReturn(nextLesson);
    }

    private PublishedLessonRow publishedLesson(long lessonId) {
        return new PublishedLessonRow(
                lessonId,
                "第一课",
                "第一课摘要",
                "## 概念\n正文",
                8,
                COURSE_ID,
                "股票投资基础",
                11L,
                "第一章"
        );
    }

    private UserLessonProgressEntity progress(
            long userId,
            long lessonId,
            LocalDateTime completedAt
    ) {
        UserLessonProgressEntity entity = new UserLessonProgressEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setLessonId(lessonId);
        entity.setCompletedAt(completedAt);
        entity.setCreatedAt(completedAt);
        return entity;
    }

    private NextLessonRow nextLesson(long lessonId, String title) {
        return new NextLessonRow(
                lessonId,
                title,
                title + "摘要",
                8,
                11L,
                "第一章"
        );
    }
}
