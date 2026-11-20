package com.example.epc.datascope;

import lombok.Value;

import java.util.Set;

/**
 * 解析后的数据范围上下文：当前登录用户「能看见哪些部门的数据、
 * 是否只能看本人」的运行时快照。
 *
 * <p>由 {@link DataScopeResolver} 在每次需要时解析并缓存
 * （{@link com.example.epc.permission.PermissionCache}），
 * 数据权限拦截器（SQL 注入）和越权校验器（数据归属检查）共用同一份
 * 口径——保证「列表过滤」和「详情校验」不会各说各话。</p>
 */
@Value
public class DataScopeContext {

    /** 生效的数据范围类型（多角色取最宽） */
    DataScopeType type;

    /**
     * 可见部门 id 集合。
     * ALL 时为 null（不加部门条件）；SELF 时为空集（按 user_id 过滤）。
     */
    Set<Long> deptIds;

    /** 当前用户 id，SELF 范围用它拼 user_id 条件 */
    Long userId;
}
