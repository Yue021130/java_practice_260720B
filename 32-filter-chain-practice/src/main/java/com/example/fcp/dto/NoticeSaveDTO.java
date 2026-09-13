package com.example.fcp.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 公告新增/编辑入参。
 */
@Data
public class NoticeSaveDTO implements Serializable {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 个字符")
    private String title;

    @Min(value = 1, message = "类型只能为 1/2/3")
    @Max(value = 3, message = "类型只能为 1/2/3")
    private Integer type;

    @Min(value = 0, message = "状态只能为 0/1")
    @Max(value = 1, message = "状态只能为 0/1")
    private Integer status;

    @Size(max = 2000, message = "正文不能超过 2000 个字符")
    private String content;

    @Min(value = 0, message = "排序号不能为负数")
    private Integer sortOrder;
}
