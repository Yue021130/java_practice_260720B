package com.example.epc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 部门保存入参：新增 / 更新共用。
 */
@Data
public class DeptSaveDTO implements Serializable {

    /** 更新时必传，新增为空 */
    private Long id;

    /** 父部门 id，顶级传 0 */
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Integer sortOrder;
}
