package com.stockmentor.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.quiz.entity.QuizAttemptEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizAttemptMapper extends BaseMapper<QuizAttemptEntity> {
}
