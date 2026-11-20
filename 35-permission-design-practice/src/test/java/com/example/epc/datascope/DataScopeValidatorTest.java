package com.example.epc.datascope;

import com.example.epc.common.BusinessException;
import com.example.epc.entity.Order;
import com.example.epc.mapper.OrderMapper;
import com.example.epc.permission.PermissionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 水平越权二次校验单测：IDOR 防护。
 */
class DataScopeValidatorTest {

    private OrderMapper orderMapper;
    private PermissionCache permissionCache;
    private DataScopeValidator validator;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        permissionCache = mock(PermissionCache.class);
        validator = new DataScopeValidator(orderMapper, permissionCache);
    }

    private Order order(Long id, Long deptId, Long userId) {
        Order o = new Order();
        o.setId(id);
        o.setDeptId(deptId);
        o.setUserId(userId);
        return o;
    }

    @Test
    @DisplayName("ALL 范围：任意订单放行")
    void allPasses() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 3L, 7L));
        when(permissionCache.getDataScope(9L))
                .thenReturn(new DataScopeContext(DataScopeType.ALL, null, 9L));
        assertDoesNotThrow(() -> validator.checkOrderVisible(1L, 9L));
    }

    @Test
    @DisplayName("SELF 范围：本人订单放行")
    void selfOwnPasses() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 3L, 9L));
        when(permissionCache.getDataScope(9L))
                .thenReturn(new DataScopeContext(DataScopeType.SELF, Set.of(), 9L));
        assertDoesNotThrow(() -> validator.checkOrderVisible(1L, 9L));
    }

    @Test
    @DisplayName("SELF 范围：他人订单 403（水平越权拦截）")
    void selfOthersBlocked() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 3L, 7L));
        when(permissionCache.getDataScope(9L))
                .thenReturn(new DataScopeContext(DataScopeType.SELF, Set.of(), 9L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.checkOrderVisible(1L, 9L));
        assertTrue(e.getMessage().contains("越权"));
    }

    @Test
    @DisplayName("DEPT 范围：部门内订单放行、部门外订单 403")
    void deptScope() {
        when(permissionCache.getDataScope(9L))
                .thenReturn(new DataScopeContext(DataScopeType.DEPT, Set.of(2L), 9L));
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 2L, 7L));
        assertDoesNotThrow(() -> validator.checkOrderVisible(1L, 9L));

        when(orderMapper.selectById(2L)).thenReturn(order(2L, 4L, 8L));
        assertThrows(BusinessException.class, () -> validator.checkOrderVisible(2L, 9L));
    }

    @Test
    @DisplayName("订单不存在：报不存在而不是放行")
    void notFound() {
        when(orderMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> validator.checkOrderVisible(99L, 9L));
    }
}
