package com.example.tip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tip.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户 Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
