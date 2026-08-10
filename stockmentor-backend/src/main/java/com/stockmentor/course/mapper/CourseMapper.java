package com.stockmentor.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockmentor.course.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<CourseEntity> {
}
