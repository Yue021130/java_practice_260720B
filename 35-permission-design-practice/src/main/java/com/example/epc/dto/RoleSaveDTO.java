package com.example.epc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 角色保存入参：新增 / 更新共用，携带权限点全量与数据范围规则。
 */
@Data
public class RoleSaveDTO implements Serializable {

    /** 更新时必传，新增为空 */
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 权限点 id 全量集合，保存时先删后插 */
    private List<Long> permissionIds;

    /** 数据范围：ALL / CUSTOM / DEPT_AND_CHILD / DEPT / SELF */
    @NotBlank(message = "数据范围类型不能为空")
    private String scopeType;

    /** CUSTOM 时生效：逗号分隔的部门 id */
    private String customDeptIds;
}
