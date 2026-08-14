package com.stockmentor.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.quiz.entity.QuizEntity;
import com.stockmentor.quiz.repository.PublishedQuizRow;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizMapper extends BaseMapper<QuizEntity> {

    @Select("""
            SELECT qz.id AS quiz_id,
                   l.id AS lesson_id,
                   l.title AS lesson_title,
                   c.id AS course_id,
                   c.title AS course_title,
                   ch.id AS chapter_id,
                   ch.title AS chapter_title,
                   qz.title AS quiz_title,
                   qz.summary AS quiz_summary
            FROM quiz qz
            INNER JOIN lesson l ON l.id = qz.lesson_id
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE l.id = #{lessonId}
              AND c.published = 1
              AND l.published = 1
              AND qz.published = 1
            """)
    @ConstructorArgs({
        @Arg(column = "quiz_id", javaType = long.class, id = true),
        @Arg(column = "lesson_id", javaType = long.class),
        @Arg(column = "lesson_title", javaType = String.class),
        @Arg(column = "course_id", javaType = long.class),
        @Arg(column = "course_title", javaType = String.class),
        @Arg(column = "chapter_id", javaType = long.class),
        @Arg(column = "chapter_title", javaType = String.class),
        @Arg(column = "quiz_title", javaType = String.class),
        @Arg(column = "quiz_summary", javaType = String.class)
    })
    PublishedQuizRow selectPublishedByLessonId(@Param("lessonId") long lessonId);
}
