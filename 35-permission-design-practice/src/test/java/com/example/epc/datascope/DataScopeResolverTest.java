package com.example.epc.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.epc.entity.Dept;
import com.example.epc.entity.RoleDataScope;
import com.example.epc.entity.User;
import com.example.epc.entity.UserRole;
import com.example.epc.mapper.DeptMapper;
import com.example.epc.mapper.RoleDataScopeMapper;
import com.example.epc.mapper.RoleMapper;
import com.example.epc.mapper.UserMapper;
import com.example.epc.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 数据范围解析器单测：五级范围 + 多角色取最宽 + 部门树递归。
 * 全部 mock Mapper，不依赖数据库。
 */
class DataScopeResolverTest {

    private UserMapper userMapper;
    private UserRoleMapper userRoleMapper;
    private RoleDataScopeMapper roleDataScopeMapper;
    private DeptMapper deptMapper;
    private DataScopeResolver resolver;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        roleDataScopeMapper = mock(RoleDataScopeMapper.class);
        deptMapper = mock(DeptMapper.class);
        resolver = new DataScopeResolver(userMapper, userRoleMapper, roleMapper,
                roleDataScopeMapper, deptMapper);

        User user = new User();
        user.setId(9L);
        user.setDeptId(2L); // 研发部
        when(userMapper.selectById(9L)).thenReturn(user);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(userRole(10L)));
    }

    private UserRole userRole(Long roleId) {
        UserRole ur = new UserRole();
        ur.setUserId(9L);
        ur.setRoleId(roleId);
        return ur;
    }

    private void mockScopeRule(String scopeType, String customDeptIds) {
        RoleDataScope rule = new RoleDataScope();
        rule.setRoleId(10L);
        rule.setScopeType(scopeType);
        rule.setCustomDeptIds(customDeptIds);
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(rule));
    }

    @Test
    @DisplayName("ALL：不加部门条件（deptIds 为 null）")
    void allScope() {
        mockScopeRule("ALL", null);
        DataScopeContext ctx = resolver.resolve(9L);
        assertEquals(DataScopeType.ALL, ctx.getType());
        assertNull(ctx.getDeptIds());
    }

    @Test
    @DisplayName("DEPT：仅本部门")
    void deptScope() {
        mockScopeRule("DEPT", null);
        DataScopeContext ctx = resolver.resolve(9L);
        assertEquals(Set.of(2L), ctx.getDeptIds());
    }

    @Test
    @DisplayName("DEPT_AND_CHILD：递归收集本部门及全部子孙部门")
    void deptAndChildScope() {
        mockScopeRule("DEPT_AND_CHILD", null);
        // 部门树：1(总公司) → 2(研发部) → 5(研发一组)、6(研发二组)；3/4 是 2 的兄弟部门
        when(deptMapper.selectList(null)).thenReturn(Arrays.asList(
                dept(1L, 0L), dept(2L, 1L), dept(3L, 1L), dept(4L, 1L),
                dept(5L, 2L), dept(6L, 2L)));

        DataScopeContext ctx = resolver.resolve(9L);
        assertEquals(DataScopeType.DEPT_AND_CHILD, ctx.getType());
        assertEquals(Set.of(2L, 5L, 6L), ctx.getDeptIds(), "应包含本部门 2 及其子孙 5、6，不含兄弟部门 3/4");
    }

    private Dept dept(Long id, Long parentId) {
        Dept d = new Dept();
        d.setId(id);
        d.setParentId(parentId);
        return d;
    }

    @Test
    @DisplayName("CUSTOM：解析自定义部门 csv")
    void customScope() {
        mockScopeRule("CUSTOM", "3, 4");
        DataScopeContext ctx = resolver.resolve(9L);
        assertEquals(Set.of(3L, 4L), ctx.getDeptIds());
    }

    @Test
    @DisplayName("SELF：不解析部门，携带 userId 供拦截器拼 user_id 条件")
    void selfScope() {
        mockScopeRule("SELF", null);
        DataScopeContext ctx = resolver.resolve(9L);
        assertEquals(DataScopeType.SELF, ctx.getType());
        assertTrue(ctx.getDeptIds().isEmpty());
        assertEquals(9L, ctx.getUserId());
    }

    @Test
    @DisplayName("多角色取最宽：SELF 与 ALL 并存时生效 ALL")
    void widestWins() {
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(userRole(10L), userRole(11L)));
        RoleDataScope self = new RoleDataScope();
        self.setRoleId(10L);
        self.setScopeType("SELF");
        RoleDataScope all = new RoleDataScope();
        all.setRoleId(11L);
        all.setScopeType("ALL");
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(self, all));

        assertEquals(DataScopeType.ALL, resolver.resolve(9L).getType());
    }

    @Test
    @DisplayName("没配数据范围的角色默认 SELF（默认拒绝）")
    void defaultSelfWhenNoRule() {
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertEquals(DataScopeType.SELF, resolver.resolve(9L).getType());
    }
}
