package com.example.scp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.scp.entity.Product;

/**
 * 商品 Mapper：MyBatis-Plus 已提供通用 CRUD，本章无需手写 SQL。
 *
 * <p>逻辑删除由 @TableLogic 自动生效：deleteById 实为 UPDATE deleted=1，
 * 所有查询自动附带 deleted=0 条件。</p>
 */
public interface ProductMapper extends BaseMapper<Product> {
}
