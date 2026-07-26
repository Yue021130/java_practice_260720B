package com.example.mp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mp.entity.User;
import com.example.mp.mapper.UserMapper;
import com.example.mp.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现：继承 ServiceImpl<M, T>，自动注入 Mapper。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
