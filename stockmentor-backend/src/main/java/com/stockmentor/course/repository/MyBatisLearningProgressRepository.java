package com.stockmentor.course.repository;

import com.stockmentor.course.entity.UserLessonProgressEntity;
import com.stockmentor.course.mapper.LearningProgressMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisLearningProgressRepository implements LearningProgressRepository {

    private final LearningProgressMapper learningProgressMapper;

    public MyBatisLearningProgressRepository(
            LearningProgressMapper learningProgressMapper
    ) {
        this.learningProgressMapper = learningProgressMapper;
    }

    @Override
    public void upsertCompletion(long userId, long lessonId) {
        learningProgressMapper.upsertCompletion(userId, lessonId);
    }

    @Override
    public Optional<UserLessonProgressEntity> findCompletion(
            long userId,
            long lessonId
    ) {
        return Optional.ofNullable(
                learningProgressMapper.selectCompletion(userId, lessonId)
        );
    }

    @Override
    public long countPublishedLessons(long courseId) {
        return learningProgressMapper.countPublishedLessons(courseId);
    }

    @Override
    public long countCompletedPublishedLessons(long userId, long courseId) {
        return learningProgressMapper.countCompletedPublishedLessons(userId, courseId);
    }

    @Override
    public List<Long> findCompletedPublishedLessonIds(long userId, long courseId) {
        return learningProgressMapper.selectCompletedPublishedLessonIds(userId, courseId);
    }

    @Override
    public Optional<NextLessonRow> findNextIncompletePublishedLesson(
            long userId,
            long courseId
    ) {
        return Optional.ofNullable(
                learningProgressMapper.selectNextIncompletePublishedLesson(
                        userId,
                        courseId
                )
        );
    }
}
