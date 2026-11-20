package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单出参：订单信息 + 冗余的部门名 / 用户名，便于列表页直接展示。
 */
@Data
public class OrderVO implements Serializable {

    private Long id;

    private String orderNo;

    private BigDecimal amount;

    private Long deptId;

    private String deptName;

    private Long userId;

    private String username;

    /** 状态：1 正常，0 已取消 */
    private Integer status;

    private LocalDateTime createTime;
}
