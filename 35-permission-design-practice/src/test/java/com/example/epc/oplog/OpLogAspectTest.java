package com.example.epc.oplog;

import com.example.epc.entity.OpLog;
import com.example.epc.mapper.OpLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 操作日志切面单测：@OpLog 注解触发审计落库，业务异常照常上抛。
 */
class OpLogAspectTest {

    private OpLogMapper opLogMapper;
    private OpLogAspect aspect;

    @BeforeEach
    void setUp() {
        opLogMapper = mock(OpLogMapper.class);
        aspect = new OpLogAspect(opLogMapper);
    }

    private ProceedingJoinPoint mockPjp(Object returnValue) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringType()).thenReturn((Class) DemoController.class);
        when(signature.getName()).thenReturn("create");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"arg1"});
        when(pjp.proceed()).thenReturn(returnValue);
        return pjp;
    }

    @Test
    @DisplayName("正常返回：审计落库（标题/权限点/方法/参数），返回值原样传递")
    void success() throws Throwable {
        com.example.epc.oplog.OpLog ann = DemoController.class.getMethod("create", Object.class).getAnnotation(com.example.epc.oplog.OpLog.class);
        Object result = aspect.around(mockPjp("ok"), ann);

        assertEquals("ok", result);
        ArgumentCaptor<OpLog> captor = ArgumentCaptor.forClass(OpLog.class);
        verify(opLogMapper).insert(captor.capture());
        OpLog saved = captor.getValue();
        assertEquals("新增用户", saved.getTitle());
        assertEquals("user:create", saved.getPerm());
        assertEquals("DemoController#create", saved.getMethod());
        assertNotNull(saved.getParams());
        assertTrue(saved.getCostMs() >= 0);
    }

    @Test
    @DisplayName("业务异常：审计照常落库并标记失败，异常原样上抛")
    void failure() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringType()).thenReturn((Class) DemoController.class);
        when(signature.getName()).thenReturn("create");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        RuntimeException boom = new RuntimeException("库存不足");
        when(pjp.proceed()).thenThrow(boom);

        com.example.epc.oplog.OpLog ann = DemoController.class.getMethod("create", Object.class).getAnnotation(com.example.epc.oplog.OpLog.class);
        Throwable thrown = assertThrows(RuntimeException.class, () -> aspect.around(pjp, ann));
        assertSame(boom, thrown);

        ArgumentCaptor<OpLog> captor = ArgumentCaptor.forClass(OpLog.class);
        verify(opLogMapper).insert(captor.capture());
        assertTrue(captor.getValue().getTitle().contains("失败"));
        assertTrue(captor.getValue().getTitle().contains("库存不足"));
    }

    static class DemoController {
        @com.example.epc.oplog.OpLog(value = "新增用户", perm = "user:create")
        public String create(Object arg) {
            return "ok";
        }
    }
}
