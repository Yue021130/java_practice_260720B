package com.example.vmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vmp.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper：继承 BaseMapper 自带单表 CRUD，无需编写 XML。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
