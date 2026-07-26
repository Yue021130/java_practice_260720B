package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类：配合 User 做关联查询与分组统计实战。
 */
@Data
@TableName("t_order")
@Schema(description = "订单实体")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键，雪花 ID")
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "订单金额")
    private BigDecimal amount;

    @Schema(description = "订单状态：0-待支付，1-已支付，2-已取消")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
