package com.example.tip.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事务场景演示结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSceneVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 场景名称 */
    private String scene;

    /** 场景说明 */
    private String description;

    /** 是否回滚成功（true 表示数据一致） */
    private Boolean rollbackSuccess;

    /** 提示信息 */
    private String message;
}
