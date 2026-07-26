package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.DbType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品实体类：演示 @KeySequence 序列主键。
 *
 * 面试点：
 * - @KeySequence 用于 Oracle、PostgreSQL、DB2、H2 等支持 sequence 的数据库。
 * - value 指定序列名，dbType 指定数据库类型。
 * - @TableId 需使用 IdType.INPUT，由序列生成主键后回填。
 */
@Data
@TableName("t_product")
@KeySequence(value = "seq_product", dbType = DbType.H2)
@Schema(description = "商品实体（演示 @KeySequence）")
public class Product {

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键，由 H2 序列 seq_product 生成")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "价格")
    private BigDecimal price;
}
