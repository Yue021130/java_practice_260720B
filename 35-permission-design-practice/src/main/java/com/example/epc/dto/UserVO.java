package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户出参：剔除 password 字段，保证敏感信息不外泄。
 *
 * <p>deptName / roleIds / roleNames 为用户管理页展示与回显扩展，
 * AuthController.info() 只填充基本字段，其余为 null。</p>
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private Long deptId;

    /** 所属部门名称（管理页展示用） */
    private String deptName;

    /** 已分配的角色 id 集合 */
    private List<Long> roleIds;

    /** 已分配的角色名称（展示用） */
    private List<String> roleNames;

    private String email;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
