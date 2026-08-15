package com.stockmentor.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.entity.QuizQuestionEntity;
import com.stockmentor.quiz.repository.PublicQuizQuestionRow;
import com.stockmentor.quiz.repository.QuizScoringQuestionRow;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizQuestionMapper extends BaseMapper<QuizQuestionEntity> {

    @Select("""
            SELECT q.id AS question_id,
                   q.type AS question_type,
                   q.stem,
                   qq.sort_order
            FROM quiz_question qq
            INNER JOIN question q ON q.id = qq.question_id
            WHERE qq.quiz_id = #{quizId}
              AND q.published = 1
            ORDER BY qq.sort_order ASC, q.id ASC
            """)
    @ConstructorArgs({
        @Arg(column = "question_id", javaType = long.class, id = true),
        @Arg(column = "question_type", javaType = QuestionType.class),
        @Arg(column = "stem", javaType = String.class),
        @Arg(column = "sort_order", javaType = int.class)
    })
    List<PublicQuizQuestionRow> selectPublishedByQuizId(@Param("quizId") long quizId);

    @Select("""
            SELECT q.id AS question_id,
                   q.type AS question_type,
                   q.explanation,
                   qq.sort_order
            FROM quiz_question qq
            INNER JOIN question q ON q.id = qq.question_id
            WHERE qq.quiz_id = #{quizId}
              AND q.published = 1
            ORDER BY qq.sort_order ASC, q.id ASC
            """)
    @ConstructorArgs({
        @Arg(column = "question_id", javaType = long.class, id = true),
        @Arg(column = "question_type", javaType = QuestionType.class),
        @Arg(column = "explanation", javaType = String.class),
        @Arg(column = "sort_order", javaType = int.class)
    })
    List<QuizScoringQuestionRow> selectScoringByQuizId(@Param("quizId") long quizId);

    @Select("""
            SELECT q.id AS question_id,
                   q.type AS question_type,
                   q.explanation,
                   qq.sort_order
            FROM question q
            INNER JOIN quiz_question qq ON qq.question_id = q.id
            INNER JOIN quiz qz ON qz.id = qq.quiz_id
            INNER JOIN lesson l ON l.id = qz.lesson_id
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE q.id = #{questionId}
              AND c.published = 1
              AND l.published = 1
              AND qz.published = 1
              AND q.published = 1
            """)
    @ConstructorArgs({
        @Arg(column = "question_id", javaType = long.class, id = true),
        @Arg(column = "question_type", javaType = QuestionType.class),
        @Arg(column = "explanation", javaType = String.class),
        @Arg(column = "sort_order", javaType = int.class)
    })
    QuizScoringQuestionRow selectVisibleScoringByQuestionId(
            @Param("questionId") long questionId
    );
}
