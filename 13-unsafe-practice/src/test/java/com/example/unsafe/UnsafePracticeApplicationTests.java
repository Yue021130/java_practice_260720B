package com.example.unsafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试：验证 Unsafe Bean 能通过反射正常装配。
 */
@SpringBootTest
class UnsafePracticeApplicationTests {

    @Test
    void contextLoads() {
        // 只要能启动（Unsafe 反射装配成功、所有 Controller 扫描正常）即通过
    }
}
