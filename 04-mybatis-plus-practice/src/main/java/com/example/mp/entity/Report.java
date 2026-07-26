package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 报表实体：演示动态表名（按月分表）。
 *
 * 面试八股：
 * - 逻辑表名固定为 t_report，实际表名由 DynamicTableNameInnerInterceptor 根据上下文替换
 * - 常用于日志、订单按年月分表
 * - 注意：动态表名插件只替换 SQL 中的表名，不会自动建表，需要提前创建好物理表
 */
@Data
@TableName("t_report")
public class Report {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reportMonth;

    private String content;
}
