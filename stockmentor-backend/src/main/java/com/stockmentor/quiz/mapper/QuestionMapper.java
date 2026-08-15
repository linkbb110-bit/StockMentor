package com.stockmentor.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.quiz.entity.QuestionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<QuestionEntity> {
}
