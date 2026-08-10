package com.stockmentor.course.repository;

import com.stockmentor.course.entity.ChapterEntity;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.LessonEntity;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    List<CourseEntity> findPublishedCourses();

    Optional<CourseEntity> findPublishedCourseById(long courseId);

    List<ChapterEntity> findChaptersByCourseId(long courseId);

    List<LessonEntity> findPublishedLessonsByCourseId(long courseId);

    Optional<PublishedLessonRow> findPublishedLessonById(long lessonId);
}
