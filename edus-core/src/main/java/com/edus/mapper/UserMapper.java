package com.edus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edus.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}