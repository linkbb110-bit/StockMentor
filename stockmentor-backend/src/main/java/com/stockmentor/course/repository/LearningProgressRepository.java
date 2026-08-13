package com.stockmentor.course.repository;

import com.stockmentor.course.entity.UserLessonProgressEntity;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository {

    void upsertCompletion(long userId, long lessonId);

    Optional<UserLessonProgressEntity> findCompletion(long userId, long lessonId);

    long countPublishedLessons(long courseId);

    long countCompletedPublishedLessons(long userId, long courseId);

    List<Long> findCompletedPublishedLessonIds(long userId, long courseId);

    Optional<NextLessonRow> findNextIncompletePublishedLesson(
            long userId,
            long courseId
    );
}
