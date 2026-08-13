package com.stockmentor.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.course.entity.LessonEntity;
import com.stockmentor.course.repository.PublishedLessonRow;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LessonMapper extends BaseMapper<LessonEntity> {

    @Select("""
            SELECT l.id,
                   l.chapter_id,
                   l.title,
                   l.summary,
                   l.content_md,
                   l.estimated_minutes,
                   l.sort_order,
                   l.published,
                   l.created_at,
                   l.updated_at
            FROM lesson l
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            WHERE ch.course_id = #{courseId}
              AND l.published = 1
            ORDER BY ch.sort_order ASC, l.sort_order ASC, ch.id ASC, l.id ASC
            """)
    @Results({
        @Result(column = "id", property = "id", id = true),
        @Result(column = "chapter_id", property = "chapterId"),
        @Result(column = "title", property = "title"),
        @Result(column = "summary", property = "summary"),
        @Result(column = "content_md", property = "contentMd"),
        @Result(column = "estimated_minutes", property = "estimatedMinutes"),
        @Result(column = "sort_order", property = "sortOrder"),
        @Result(column = "published", property = "published"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<LessonEntity> selectPublishedByCourseId(@Param("courseId") long courseId);

    @Select("""
            SELECT l.id AS lesson_id,
                   l.title AS lesson_title,
                   l.summary AS lesson_summary,
                   l.content_md,
                   l.estimated_minutes,
                   c.id AS course_id,
                   c.title AS course_title,
                   ch.id AS chapter_id,
                   ch.title AS chapter_title
            FROM lesson l
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE l.id = #{lessonId}
              AND l.published = 1
              AND c.published = 1
            """)
    @ConstructorArgs({
        @Arg(column = "lesson_id", javaType = long.class, id = true),
        @Arg(column = "lesson_title", javaType = String.class),
        @Arg(column = "lesson_summary", javaType = String.class),
        @Arg(column = "content_md", javaType = String.class),
        @Arg(column = "estimated_minutes", javaType = int.class),
        @Arg(column = "course_id", javaType = long.class),
        @Arg(column = "course_title", javaType = String.class),
        @Arg(column = "chapter_id", javaType = long.class),
        @Arg(column = "chapter_title", javaType = String.class)
    })
    PublishedLessonRow selectPublishedDetailsById(@Param("lessonId") long lessonId);
}
