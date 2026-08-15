package com.stockmentor.quiz.mapper;

import com.stockmentor.quiz.domain.QuestionType;
import com.stockmentor.quiz.domain.WrongQuestionStatus;
import com.stockmentor.quiz.entity.WrongQuestionEntity;
import com.stockmentor.quiz.repository.WrongQuestionRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WrongQuestionMapper {

    @Insert("""
            INSERT INTO wrong_question (
                user_id,
                question_id,
                status,
                error_count,
                last_wrong_at,
                mastered_at,
                last_reviewed_at
            ) VALUES (
                #{userId},
                #{questionId},
                'PENDING',
                1,
                #{reviewedAt},
                NULL,
                #{reviewedAt}
            )
            ON DUPLICATE KEY UPDATE
                status = 'PENDING',
                error_count = error_count + 1,
                last_wrong_at = VALUES(last_wrong_at),
                mastered_at = NULL,
                last_reviewed_at = VALUES(last_reviewed_at),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertWrong(
            @Param("userId") long userId,
            @Param("questionId") long questionId,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    @Update("""
            UPDATE wrong_question
            SET mastered_at = CASE
                    WHEN status = 'PENDING' THEN #{reviewedAt}
                    ELSE mastered_at
                END,
                status = 'MASTERED',
                last_reviewed_at = #{reviewedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId}
              AND question_id = #{questionId}
            """)
    int updateCorrect(
            @Param("userId") long userId,
            @Param("questionId") long questionId,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    @Select("""
            SELECT id,
                   user_id,
                   question_id,
                   status,
                   error_count,
                   last_wrong_at,
                   mastered_at,
                   last_reviewed_at,
                   created_at,
                   updated_at
            FROM wrong_question
            WHERE user_id = #{userId}
              AND question_id = #{questionId}
            FOR UPDATE
            """)
    @Results({
        @Result(column = "id", property = "id", id = true),
        @Result(column = "user_id", property = "userId"),
        @Result(column = "question_id", property = "questionId"),
        @Result(column = "status", property = "status"),
        @Result(column = "error_count", property = "errorCount"),
        @Result(column = "last_wrong_at", property = "lastWrongAt"),
        @Result(column = "mastered_at", property = "masteredAt"),
        @Result(column = "last_reviewed_at", property = "lastReviewedAt"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    WrongQuestionEntity selectStateForUpdate(
            @Param("userId") long userId,
            @Param("questionId") long questionId
    );

    @Select("""
            SELECT wq.id,
                   q.id AS question_id,
                   l.id AS lesson_id,
                   l.title AS lesson_title,
                   q.type AS question_type,
                   q.stem,
                   wq.status,
                   wq.error_count,
                   wq.last_wrong_at,
                   wq.mastered_at,
                   wq.last_reviewed_at
            FROM wrong_question wq
            INNER JOIN question q ON q.id = wq.question_id
            INNER JOIN quiz_question qq ON qq.question_id = q.id
            INNER JOIN quiz qz ON qz.id = qq.quiz_id
            INNER JOIN lesson l ON l.id = qz.lesson_id
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE wq.user_id = #{userId}
              AND wq.status = #{status}
              AND c.published = 1
              AND l.published = 1
              AND qz.published = 1
              AND q.published = 1
            ORDER BY
                CASE WHEN #{status} = 'PENDING' THEN wq.last_wrong_at END DESC,
                CASE WHEN #{status} = 'MASTERED' THEN wq.mastered_at END DESC,
                wq.id DESC
            """)
    @ConstructorArgs({
        @Arg(column = "id", javaType = long.class, id = true),
        @Arg(column = "question_id", javaType = long.class),
        @Arg(column = "lesson_id", javaType = long.class),
        @Arg(column = "lesson_title", javaType = String.class),
        @Arg(column = "question_type", javaType = QuestionType.class),
        @Arg(column = "stem", javaType = String.class),
        @Arg(column = "status", javaType = WrongQuestionStatus.class),
        @Arg(column = "error_count", javaType = int.class),
        @Arg(column = "last_wrong_at", javaType = LocalDateTime.class),
        @Arg(column = "mastered_at", javaType = LocalDateTime.class),
        @Arg(column = "last_reviewed_at", javaType = LocalDateTime.class)
    })
    List<WrongQuestionRow> selectVisibleByStatus(
            @Param("userId") long userId,
            @Param("status") WrongQuestionStatus status
    );

    @Select("""
            SELECT wq.id,
                   q.id AS question_id,
                   l.id AS lesson_id,
                   l.title AS lesson_title,
                   q.type AS question_type,
                   q.stem,
                   wq.status,
                   wq.error_count,
                   wq.last_wrong_at,
                   wq.mastered_at,
                   wq.last_reviewed_at
            FROM wrong_question wq
            INNER JOIN question q ON q.id = wq.question_id
            INNER JOIN quiz_question qq ON qq.question_id = q.id
            INNER JOIN quiz qz ON qz.id = qq.quiz_id
            INNER JOIN lesson l ON l.id = qz.lesson_id
            INNER JOIN chapter ch ON ch.id = l.chapter_id
            INNER JOIN course c ON c.id = ch.course_id
            WHERE wq.user_id = #{userId}
              AND wq.question_id = #{questionId}
              AND c.published = 1
              AND l.published = 1
              AND qz.published = 1
              AND q.published = 1
            """)
    @ConstructorArgs({
        @Arg(column = "id", javaType = long.class, id = true),
        @Arg(column = "question_id", javaType = long.class),
        @Arg(column = "lesson_id", javaType = long.class),
        @Arg(column = "lesson_title", javaType = String.class),
        @Arg(column = "question_type", javaType = QuestionType.class),
        @Arg(column = "stem", javaType = String.class),
        @Arg(column = "status", javaType = WrongQuestionStatus.class),
        @Arg(column = "error_count", javaType = int.class),
        @Arg(column = "last_wrong_at", javaType = LocalDateTime.class),
        @Arg(column = "mastered_at", javaType = LocalDateTime.class),
        @Arg(column = "last_reviewed_at", javaType = LocalDateTime.class)
    })
    WrongQuestionRow selectVisibleByQuestion(
            @Param("userId") long userId,
            @Param("questionId") long questionId
    );
}
