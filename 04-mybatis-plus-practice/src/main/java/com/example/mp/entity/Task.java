package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体：演示自定义注解 + AOP 自动填充 createBy / updateBy。
 */
@Data
@TableName("t_task")
@Schema(description = "任务实体（演示自定义注解 + AOP）")
public class Task {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "任务内容")
    private String content;

    @Schema(description = "状态：0-待处理，1-已完成")
    private Integer status;

    @Schema(description = "创建人：由自定义注解 + AOP 自动填充")
    private String createBy;

    @Schema(description = "更新人：由自定义注解 + AOP 自动填充")
    private String updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
