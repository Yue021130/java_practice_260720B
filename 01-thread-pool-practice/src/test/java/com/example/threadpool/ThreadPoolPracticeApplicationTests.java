package com.example.threadpool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文加载冒烟测试。
 *
 * 只要 Spring 容器能正常启动（所有 Bean 创建成功、无注入歧义），
 * 本测试即通过——这是排查配置错误最快速的手段。
 */
@SpringBootTest
class ThreadPoolPracticeApplicationTests {

    @Test
    void contextLoads() {
        // 无需断言：上下文加载失败时测试自动失败
    }
}
