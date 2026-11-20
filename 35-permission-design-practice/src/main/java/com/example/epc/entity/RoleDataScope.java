package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色数据范围实体，对应表 sys_role_data_scope。
 *
 * <p>一个角色一条规则：scopeType 存 ALL / CUSTOM / DEPT_AND_CHILD / DEPT / SELF，
 * 见 datascope/DataScopeType；CUSTOM 时 customDeptIds 存逗号分隔的部门 id。</p>
 */
@Data
@TableName("sys_role_data_scope")
public class RoleDataScope {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    /** 数据范围类型：ALL / CUSTOM / DEPT_AND_CHILD / DEPT / SELF */
    private String scopeType;

    /** CUSTOM 时生效：逗号分隔的部门 id，如 "2,3"；其余类型为空 */
    private String customDeptIds;
}
