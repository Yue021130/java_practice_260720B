package com.example.mp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mp.entity.User;

/**
 * 用户 Service 接口：继承 IService 获得 Service 层 CRUD 能力。
 *
 * 面试点：
 * - IService<T> 提供了 save、saveOrUpdate、saveBatch、remove、removeById、removeByMap、update、updateById、getById、list、page 等方法。
 * - Service 层适合封装业务逻辑，Mapper 层专注数据访问。
 */
public interface UserService extends IService<User> {
}
