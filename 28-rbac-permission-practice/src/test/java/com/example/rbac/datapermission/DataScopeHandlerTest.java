package com.example.rbac.datapermission;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 数据权限拦截器单元测试：
 * 验证不同 data_scope 下 WHERE 条件的注入行为
 */
class DataScopeHandlerTest {

    private DataScopeContext dataScopeContext;
    private DataScopeHandler handler;

    @BeforeEach
    void setUp() {
        dataScopeContext = mock(DataScopeContext.class);
        handler = new DataScopeHandler(dataScopeContext);
    }

    @Test
    void globalScope_shouldNotAppendCondition() {
        when(dataScopeContext.current()).thenReturn(DataScopeInfo.global());
        Expression where = baseWhere();
        Expression result = handler.getSqlSegment(where, "com.example.rbac.mapper.BizOrderMapper.selectPage");
        assertSame(where, result);
    }

    @Test
    void deptScope_shouldAppendDeptCondition() {
        when(dataScopeContext.current()).thenReturn(new DataScopeInfo(2, 3L, 1L));
        Expression result = handler.getSqlSegment(baseWhere(), "com.example.rbac.mapper.BizOrderMapper.selectPage");
        String sql = result.toString();
        assertTrue(sql.contains("status = 'PAID'"), "原条件应保留: " + sql);
        assertTrue(sql.contains("dept_id = 1"), "应注入部门条件: " + sql);
        assertFalse(sql.contains("user_id"), "部门范围不应过滤 user_id: " + sql);
    }

    @Test
    void selfScope_shouldAppendDeptAndUserCondition() {
        when(dataScopeContext.current()).thenReturn(new DataScopeInfo(1, 3L, 1L));
        Expression result = handler.getSqlSegment(baseWhere(), "com.example.rbac.mapper.BizOrderMapper.selectPage");
        String sql = result.toString();
        assertTrue(sql.contains("dept_id = 1"), "应注入部门条件: " + sql);
        assertTrue(sql.contains("user_id = 3"), "本人范围应追加 user_id 条件: " + sql);
    }

    @Test
    void notGuardedMapper_shouldNotAppendCondition() {
        when(dataScopeContext.current()).thenReturn(new DataScopeInfo(1, 3L, 1L));
        Expression where = baseWhere();
        Expression result = handler.getSqlSegment(where, "com.example.rbac.mapper.SysUserMapper.selectPage");
        assertSame(where, result);
    }

    @Test
    void nullWhere_shouldCreateNewCondition() {
        when(dataScopeContext.current()).thenReturn(new DataScopeInfo(2, 3L, 1L));
        Expression result = handler.getSqlSegment(null, "com.example.rbac.mapper.BizOrderMapper.selectPage");
        assertEquals("dept_id = 1", result.toString());
    }

    private Expression baseWhere() {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column("status"));
        equalsTo.setRightExpression(new StringValue("PAID"));
        return equalsTo;
    }
}
