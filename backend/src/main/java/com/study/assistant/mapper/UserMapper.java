package com.study.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.assistant.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}