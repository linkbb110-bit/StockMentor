package com.stockmentor.course.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.course.entity.UserLessonProgressEntity;
import com.stockmentor.course.repository.CourseRepository;
import com.stockmentor.course.repository.LearningProgressRepository;
import com.stockmentor.course.repository.NextLessonRow;
import com.stockmentor.course.vo.CourseProgressResponse;
import com.stockmentor.course.vo.LessonCompletionResponse;
import com.stockmentor.course.vo.NextLessonResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningProgressService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final CourseRepository courseRepository;
    private final LearningProgressRepository learningProgressRepository;

    public LearningProgressService(
            CourseRepository courseRepository,
            LearningProgressRepository learningProgressRepository
    ) {
        this.courseRepository = courseRepository;
        this.learningProgressRepository = learningProgressRepository;
    }

    @Transactional
    public LessonCompletionResponse completeLesson(long userId, long lessonId) {
        courseRepository.findPublishedLessonById(lessonId)
                .orElseThrow(this::resourceNotFound);

        learningProgressRepository.upsertCompletion(userId, lessonId);
        UserLessonProgressEntity progress = learningProgressRepository
                .findCompletion(userId, lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));

        return new LessonCompletionResponse(
                progress.getLessonId(),
                true,
                progress.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(long userId, long courseId) {
        courseRepository.findPublishedCourseById(courseId)
                .orElseThrow(this::resourceNotFound);

        long totalLessons = learningProgressRepository.countPublishedLessons(courseId);
        long completedLessons = learningProgressRepository
                .countCompletedPublishedLessons(userId, courseId);
        List<Long> completedLessonIds = List.copyOf(
                learningProgressRepository.findCompletedPublishedLessonIds(
                        userId,
                        courseId
                )
        );
        NextLessonResponse nextLesson = learningProgressRepository
                .findNextIncompletePublishedLesson(userId, courseId)
                .map(this::toNextLessonResponse)
                .orElse(null);

        return new CourseProgressResponse(
                completedLessons,
                totalLessons,
                progressPercent(completedLessons, totalLessons),
                completedLessonIds,
                nextLesson
        );
    }

    private int progressPercent(long completedLessons, long totalLessons) {
        if (totalLessons == 0) {
            return 0;
        }
        return BigDecimal.valueOf(completedLessons)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalLessons), 0, RoundingMode.DOWN)
                .intValueExact();
    }

    private NextLessonResponse toNextLessonResponse(NextLessonRow lesson) {
        return new NextLessonResponse(
                lesson.id(),
                lesson.title(),
                lesson.summary(),
                lesson.estimatedMinutes(),
                lesson.chapterId(),
                lesson.chapterTitle()
        );
    }

    private BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
