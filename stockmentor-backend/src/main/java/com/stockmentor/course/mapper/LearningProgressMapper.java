package com.stockmentor.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.course.entity.UserLessonProgressEntity;
import com.stockmentor.course.repository.NextLessonRow;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LearningProgressMapper extends BaseMapper<UserLessonProgressEntity> {

    @Insert("""
            INSERT INTO user_lesson_progress (user_id, lesson_id, completed_at)
            VALUES (#{userId}, #{lessonId}, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int upsertCompletion(
            @Param("userId") long userId,
            @Param("lessonId") long lessonId
    );

    @Select("""
            SELECT id, user_id, lesson_id, completed_at, created_at
            FROM user_lesson_progress
            WHERE user_id = #{userId}
              AND lesson_id = #{lessonId}
            FOR UPDATE
            """)
    @Results({
        @Result(column = "id", property = "id", id = true),
        @Result(column = "user_id", property = "userId"),
        @Result(column = "lesson_id", property = "lessonId"),
        @Result(column = "completed_at", property = "completedAt"),
        @Result(column = "created_at", property = "createdAt")
    })
    UserLessonProgressEntity selectCompletion(
            @Param("userId") long userId,
            @Param("lessonId") long lessonId
    );

    @Select("""
            SELECT COUNT(*)
            FROM lesson l
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE c.id = #{courseId}
              AND c.published = 1
              AND l.published = 1
            """)
    long countPublishedLessons(@Param("courseId") long courseId);

    @Select("""
            SELECT COUNT(*)
            FROM user_lesson_progress ulp
            INNER JOIN lesson l
                    ON l.id = ulp.lesson_id
                   AND l.published = 1
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c
                    ON c.id = ch.course_id
                   AND c.published = 1
            WHERE ulp.user_id = #{userId}
              AND c.id = #{courseId}
            """)
    long countCompletedPublishedLessons(
            @Param("userId") long userId,
            @Param("courseId") long courseId
    );

    @Select("""
            SELECT l.id
            FROM user_lesson_progress ulp
            INNER JOIN lesson l
                    ON l.id = ulp.lesson_id
                   AND l.published = 1
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c
                    ON c.id = ch.course_id
                   AND c.published = 1
            WHERE ulp.user_id = #{userId}
              AND c.id = #{courseId}
            ORDER BY ch.sort_order ASC,
                     l.sort_order ASC,
                     ch.id ASC,
                     l.id ASC
            """)
    List<Long> selectCompletedPublishedLessonIds(
            @Param("userId") long userId,
            @Param("courseId") long courseId
    );

    @Select("""
            SELECT l.id AS lesson_id,
                   l.title AS lesson_title,
                   l.summary AS lesson_summary,
                   l.estimated_minutes,
                   ch.id AS chapter_id,
                   ch.title AS chapter_title
            FROM lesson l
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c
                    ON c.id = ch.course_id
                   AND c.published = 1
            WHERE c.id = #{courseId}
              AND l.published = 1
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_lesson_progress ulp
                  WHERE ulp.user_id = #{userId}
                    AND ulp.lesson_id = l.id
              )
            ORDER BY ch.sort_order ASC,
                     l.sort_order ASC,
                     ch.id ASC,
                     l.id ASC
            LIMIT 1
            """)
    @ConstructorArgs({
        @Arg(column = "lesson_id", javaType = long.class, id = true),
        @Arg(column = "lesson_title", javaType = String.class),
        @Arg(column = "lesson_summary", javaType = String.class),
        @Arg(column = "estimated_minutes", javaType = int.class),
        @Arg(column = "chapter_id", javaType = long.class),
        @Arg(column = "chapter_title", javaType = String.class)
    })
    NextLessonRow selectNextIncompletePublishedLesson(
            @Param("userId") long userId,
            @Param("courseId") long courseId
    );
}
