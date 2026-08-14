package com.stockmentor.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.quiz.entity.QuestionOptionEntity;
import com.stockmentor.quiz.repository.PublicQuizOptionRow;
import com.stockmentor.quiz.repository.QuizScoringOptionRow;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOptionEntity> {

    @Select("""
            <script>
            SELECT qo.id AS option_id,
                   qo.question_id,
                   qo.option_key,
                   qo.content,
                   qo.sort_order
            FROM question_option qo
            WHERE qo.published = 1
              AND qo.question_id IN
              <foreach collection="questionIds" item="questionId" open="(" separator="," close=")">
                #{questionId}
              </foreach>
            ORDER BY qo.question_id ASC, qo.sort_order ASC, qo.id ASC
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "option_id", javaType = long.class, id = true),
        @Arg(column = "question_id", javaType = long.class),
        @Arg(column = "option_key", javaType = String.class),
        @Arg(column = "content", javaType = String.class),
        @Arg(column = "sort_order", javaType = int.class)
    })
    List<PublicQuizOptionRow> selectPublishedByQuestionIds(
            @Param("questionIds") List<Long> questionIds
    );

    @Select("""
            <script>
            SELECT qo.id AS option_id,
                   qo.question_id,
                   qo.is_correct,
                   qo.sort_order
            FROM question_option qo
            WHERE qo.published = 1
              AND qo.question_id IN
              <foreach collection="questionIds" item="questionId" open="(" separator="," close=")">
                #{questionId}
              </foreach>
            ORDER BY qo.question_id ASC, qo.sort_order ASC, qo.id ASC
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "option_id", javaType = long.class, id = true),
        @Arg(column = "question_id", javaType = long.class),
        @Arg(column = "is_correct", javaType = boolean.class),
        @Arg(column = "sort_order", javaType = int.class)
    })
    List<QuizScoringOptionRow> selectScoringByQuestionIds(
            @Param("questionIds") List<Long> questionIds
    );
}
