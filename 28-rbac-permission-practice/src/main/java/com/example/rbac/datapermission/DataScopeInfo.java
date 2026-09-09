package com.example.rbac.datapermission;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户的数据权限上下文
 * scope：1=本人（SELF） 2=本部门（DEPT） 3=全部数据（GLOBAL）
 */
@Data
@AllArgsConstructor
public class DataScopeInfo {

    /** 数据权限范围：1本人 2本部门 3全部 */
    private int scope;

    /** 当前用户 ID（SELF 范围下过滤 user_id 用） */
    private long userId;

    /** 当前用户部门 ID（DEPT 范围下过滤 dept_id 用） */
    private long deptId;

    public static DataScopeInfo global() {
        return new DataScopeInfo(3, 0L, 0L);
    }

    public boolean isGlobal() {
        return scope >= 3;
    }

    public boolean isSelf() {
        return scope <= 1;
    }
}
