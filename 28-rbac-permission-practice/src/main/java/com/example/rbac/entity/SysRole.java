package com.example.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，如 ADMIN */
    private String code;

    /** 角色名称 */
    private String name;

    /** 数据权限范围：1本人 2本部门 3全部 */
    private Integer dataScope;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
