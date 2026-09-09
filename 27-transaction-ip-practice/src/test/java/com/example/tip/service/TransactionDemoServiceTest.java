package com.example.tip.service;

import com.example.tip.dto.TransactionSceneVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务失效场景测试
 */
@SpringBootTest
class TransactionDemoServiceTest {

    @Autowired
    private TransactionDemoService transactionDemoService;

    @Test
    void testAllScenes() throws Exception {
        List<TransactionSceneVO> scenes = Arrays.asList(
                transactionDemoService.scene1SelfInvoke(),
                transactionDemoService.scene1Fix(),
                transactionDemoService.scene2SwallowException(),
                transactionDemoService.scene3WrongRollbackFor(),
                transactionDemoService.scene4NonPublic(),
                transactionDemoService.scene5Async()
        );

        // 场景1：同类自调用 => 事务失效（不回滚）
        assertFalse(scenes.get(0).getRollbackSuccess(), "同类自调用应该事务失效");

        // 场景1 修复：通过代理对象调用 => 事务生效
        assertTrue(scenes.get(1).getRollbackSuccess(), "代理对象调用应该事务生效");

        // 场景2：异常被吞 => 事务失效
        assertFalse(scenes.get(2).getRollbackSuccess(), "异常被吞应该事务失效");

        // 场景3：rollbackFor 不匹配 => 事务失效
        assertFalse(scenes.get(3).getRollbackSuccess(), "rollbackFor 不匹配应该事务失效");

        // 场景4：非 public 方法 => 事务失效
        assertFalse(scenes.get(4).getRollbackSuccess(), "非 public 方法应该事务失效");

        // 场景5：@Async 异步事务 => 自身会回滚
        assertTrue(scenes.get(5).getRollbackSuccess(), "异步方法自身事务应该回滚");
    }
}
