package com.example.epc.datascope;

import lombok.Getter;

/**
 * 数据范围五级模型（若依/生产后台通用口径）。
 *
 * <p>一个用户可能拥有多个角色，各角色配置了不同范围，最终生效范围
 * 取「最宽」的一个（{@link #isWiderThan}），这与若依的做法一致：
 * 多角色不叠加取交集——交集会让权限越配越小，管理员排查困难。</p>
 *
 * <p>枚举顺序即宽窄顺序：ordinal 越小范围越宽。</p>
 */
@Getter
public enum DataScopeType {

    /** 全部数据 */
    ALL("全部数据", true),
    /** 自定义部门（sys_role_data_scope.custom_dept_ids 指定） */
    CUSTOM("自定义数据", true),
    /** 本部门及以下子部门（递归部门树） */
    DEPT_AND_CHILD("本部门及以下", true),
    /** 仅本部门 */
    DEPT("本部门", true),
    /** 仅本人创建的数据 */
    SELF("仅本人", false);

    private final String label;
    /** 该范围是否需要按部门过滤；SELF 按 user_id 过滤 */
    private final boolean deptBased;

    DataScopeType(String label, boolean deptBased) {
        this.label = label;
        this.deptBased = deptBased;
    }

    /** other 比当前更宽时返回 true（ordinal 更小 = 更宽） */
    public boolean isWiderThan(DataScopeType other) {
        return other == null || this.ordinal() < other.ordinal();
    }
}
