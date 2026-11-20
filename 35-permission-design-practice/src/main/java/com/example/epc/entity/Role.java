package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体，对应表 sys_role。
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码（唯一），如 ADMIN / RD_MANAGER / STAFF，代码里做逻辑判断用它 */
    private String roleCode;

    /** 角色名称，如「超级管理员」 */
    private String roleName;

    /** 逻辑删除：0 未删，1 已删 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
