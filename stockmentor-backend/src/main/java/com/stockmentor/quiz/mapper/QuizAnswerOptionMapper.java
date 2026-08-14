package com.stockmentor.quiz.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuizAnswerOptionMapper {

    @Insert("""
            INSERT INTO quiz_answer_option (quiz_answer_id, option_id)
            VALUES (#{quizAnswerId}, #{optionId})
            """)
    int insertSelection(
            @Param("quizAnswerId") long quizAnswerId,
            @Param("optionId") long optionId
    );
}
