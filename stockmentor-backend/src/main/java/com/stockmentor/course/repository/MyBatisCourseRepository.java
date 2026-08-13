package com.stockmentor.course.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stockmentor.course.entity.ChapterEntity;
import com.stockmentor.course.entity.CourseEntity;
import com.stockmentor.course.entity.LessonEntity;
import com.stockmentor.course.mapper.ChapterMapper;
import com.stockmentor.course.mapper.CourseMapper;
import com.stockmentor.course.mapper.LessonMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisCourseRepository implements CourseRepository {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final LessonMapper lessonMapper;

    public MyBatisCourseRepository(
            CourseMapper courseMapper,
            ChapterMapper chapterMapper,
            LessonMapper lessonMapper
    ) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
    }

    @Override
    public List<CourseEntity> findPublishedCourses() {
        LambdaQueryWrapper<CourseEntity> query = Wrappers.lambdaQuery(CourseEntity.class)
                .eq(CourseEntity::getPublished, true)
                .orderByAsc(CourseEntity::getSortOrder, CourseEntity::getId);
        return courseMapper.selectList(query);
    }

    @Override
    public Optional<CourseEntity> findPublishedCourseById(long courseId) {
        LambdaQueryWrapper<CourseEntity> query = Wrappers.lambdaQuery(CourseEntity.class)
                .eq(CourseEntity::getId, courseId)
                .eq(CourseEntity::getPublished, true);
        return Optional.ofNullable(courseMapper.selectOne(query));
    }

    @Override
    public List<ChapterEntity> findChaptersByCourseId(long courseId) {
        LambdaQueryWrapper<ChapterEntity> query = Wrappers.lambdaQuery(ChapterEntity.class)
                .eq(ChapterEntity::getCourseId, courseId)
                .orderByAsc(ChapterEntity::getSortOrder, ChapterEntity::getId);
        return chapterMapper.selectList(query);
    }

    @Override
    public List<LessonEntity> findPublishedLessonsByCourseId(long courseId) {
        return lessonMapper.selectPublishedByCourseId(courseId);
    }

    @Override
    public Optional<PublishedLessonRow> findPublishedLessonById(long lessonId) {
        return Optional.ofNullable(lessonMapper.selectPublishedDetailsById(lessonId));
    }
}
