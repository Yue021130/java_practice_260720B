package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色出参：角色基本信息 + 已分配的权限点 / 数据范围规则。
 */
@Data
public class RoleVO implements Serializable {

    private Long id;

    private String roleCode;

    private String roleName;

    /** 已分配的权限点 id 集合 */
    private List<Long> permissionIds;

    /** 数据范围类型：ALL / CUSTOM / DEPT_AND_CHILD / DEPT / SELF */
    private String scopeType;

    /** CUSTOM 时的自定义部门 id（csv） */
    private String customDeptIds;

    private LocalDateTime createTime;
}
