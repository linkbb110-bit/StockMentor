package com.stockmentor.course.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import com.stockmentor.course.entity.ChapterEntity;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.LessonEntity;
import com.stockmentor.course.repository.CourseRepository;
import com.stockmentor.course.repository.PublishedLessonRow;
import com.stockmentor.course.vo.ChapterSummaryResponse;
import com.stockmentor.course.vo.CourseDetailResponse;
import com.stockmentor.course.vo.CourseSummaryResponse;
import com.stockmentor.course.vo.LessonDetailResponse;
import com.stockmentor.course.vo.LessonSummaryResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;

    public CourseQueryService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseSummaryResponse> listPublishedCourses() {
        return courseRepository.findPublishedCourses().stream()
                .map(this::toCourseSummary)
                .toList();
    }

    public CourseDetailResponse getPublishedCourse(long courseId) {
        CourseEntity course = courseRepository.findPublishedCourseById(courseId)
                .orElseThrow(this::resourceNotFound);
        List<ChapterEntity> chapters = courseRepository.findChaptersByCourseId(courseId);
        List<LessonEntity> lessons =
                courseRepository.findPublishedLessonsByCourseId(courseId);

        Map<Long, List<LessonSummaryResponse>> lessonsByChapter = new LinkedHashMap<>();
        for (LessonEntity lesson : lessons) {
            lessonsByChapter
                    .computeIfAbsent(lesson.getChapterId(), ignored -> new ArrayList<>())
                    .add(toLessonSummary(lesson));
        }

        List<ChapterSummaryResponse> chapterResponses = chapters.stream()
                .map(chapter -> new ChapterSummaryResponse(
                        chapter.getId(),
                        chapter.getTitle(),
                        chapter.getSummary(),
                        List.copyOf(lessonsByChapter.getOrDefault(
                                chapter.getId(),
                                List.of()
                        ))
                ))
                .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCoverUrl(),
                chapterResponses
        );
    }

    public LessonDetailResponse getPublishedLesson(long lessonId) {
        PublishedLessonRow lesson = courseRepository.findPublishedLessonById(lessonId)
                .orElseThrow(this::resourceNotFound);
        return new LessonDetailResponse(
                lesson.id(),
                lesson.title(),
                lesson.summary(),
                lesson.contentMd(),
                lesson.estimatedMinutes(),
                lesson.courseId(),
                lesson.courseTitle(),
                lesson.chapterId(),
                lesson.chapterTitle()
        );
    }

    private CourseSummaryResponse toCourseSummary(CourseEntity course) {
        return new CourseSummaryResponse(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCoverUrl()
        );
    }

    private LessonSummaryResponse toLessonSummary(LessonEntity lesson) {
        return new LessonSummaryResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getEstimatedMinutes()
        );
    }

    private BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
