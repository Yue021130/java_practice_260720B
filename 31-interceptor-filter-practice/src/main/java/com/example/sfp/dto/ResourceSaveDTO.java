package com.example.sfp.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 资源新增/编辑入参。
 */
@Data
public class ResourceSaveDTO implements Serializable {

    private Long id;

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 50, message = "资源名称不能超过 50 个字符")
    private String name;

    @NotBlank(message = "资源路径不能为空")
    @Size(max = 200, message = "资源路径不能超过 200 个字符")
    private String url;

    @Min(value = 1, message = "类型只能为 1/2/3")
    @Max(value = 3, message = "类型只能为 1/2/3")
    private Integer type;

    @Min(value = 0, message = "状态只能为 0/1")
    @Max(value = 1, message = "状态只能为 0/1")
    private Integer status;

    @Min(value = 0, message = "排序号不能为负数")
    private Integer sortOrder;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
