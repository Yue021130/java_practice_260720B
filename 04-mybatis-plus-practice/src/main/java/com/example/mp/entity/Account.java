package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户实体类：演示 @TableField 高级属性。
 *
 * 面试点：
 * - select = false：查询时不出现在 SELECT 字段列表，常用于密码。
 * - condition：自定义 WHERE 条件模板，如 LIKE 拼接。
 * - update：自定义 SET 片段，如 login_count = login_count + 1。
 * - numericScale：指定 DECIMAL 小数位，影响 Java 侧 BigDecimal 精度处理。
 */
@Data
@TableName("t_account")
@Schema(description = "账户实体（演示 @TableField 高级属性）")
public class Account {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @TableField(select = false)
    @Schema(description = "密码：查询时不返回")
    private String password;

    @TableField(condition = "%s LIKE CONCAT('%%',#{%s},'%%')")
    @Schema(description = "邮箱：自定义 LIKE 条件")
    private String email;

    @TableField(update = "%s+1")
    @Schema(description = "登录次数：每次更新自动 +1")
    private Integer loginCount;

    @TableField(numericScale = "2")
    @Schema(description = "余额：保留两位小数")
    private BigDecimal balance;
}
