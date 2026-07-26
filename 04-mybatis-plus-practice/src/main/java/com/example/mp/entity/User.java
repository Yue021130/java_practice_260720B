package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.OrderBy;
import com.example.mp.entity.enums.UserGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类：演示 MyBatis-Plus 核心实体注解。
 *
 * 面试点：
 * - @TableName 指定表名，可与类名不一致。
 * - @TableId 指定主键字段与生成策略；ASSIGN_ID 是雪花 ID，AUTO 是数据库自增。
 * - @TableField 处理字段映射、排除非持久化字段、自动填充等。
 * - @TableLogic 实现逻辑删除，数据库保留记录，通过 deleted 字段标记。
 * - @Version 实现乐观锁，更新时 version 自动递增，防止并发覆盖。
 * - @EnumValue 将枚举按 code 值持久化到数据库。
 * - @OrderBy 指定默认排序字段。
 */
@Data
@TableName("t_user")
@Schema(description = "用户实体")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键，雪花 ID")
    private Long id;

    @TableField("username")
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别")
    private UserGender gender;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @TableLogic
    @Schema(description = "逻辑删除标志：0-未删除，1-已删除")
    private Integer deleted;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    @OrderBy(asc = false)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 非持久化字段：用于演示 @TableField(exist = false)。
     * 业务中常见的临时属性，如确认密码、前端选中状态等。
     */
    @TableField(exist = false)
    @Schema(description = "非持久化字段：备注")
    private String remark;
}
