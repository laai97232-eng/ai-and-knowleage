package com.study.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.assistant.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}