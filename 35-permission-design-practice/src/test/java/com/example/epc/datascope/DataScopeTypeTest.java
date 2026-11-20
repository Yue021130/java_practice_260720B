package com.example.epc.datascope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据范围宽窄比较：多角色取最宽的判定基础。
 */
class DataScopeTypeTest {

    @Test
    @DisplayName("ALL > CUSTOM > DEPT_AND_CHILD > DEPT > SELF 的宽窄顺序")
    void widthOrder() {
        assertTrue(DataScopeType.ALL.isWiderThan(DataScopeType.CUSTOM));
        assertTrue(DataScopeType.CUSTOM.isWiderThan(DataScopeType.DEPT_AND_CHILD));
        assertTrue(DataScopeType.DEPT_AND_CHILD.isWiderThan(DataScopeType.DEPT));
        assertTrue(DataScopeType.DEPT.isWiderThan(DataScopeType.SELF));
        assertFalse(DataScopeType.SELF.isWiderThan(DataScopeType.DEPT));
    }

    @Test
    @DisplayName("与 null 比较恒为更宽（首个规则直接生效）")
    void widerThanNull() {
        assertTrue(DataScopeType.SELF.isWiderThan(null));
    }

    @Test
    @DisplayName("SELF 不是部门维度，ALL 标记为部门维度但不拼条件")
    void flags() {
        assertFalse(DataScopeType.SELF.isDeptBased());
        assertTrue(DataScopeType.ALL.isDeptBased());
    }
}
