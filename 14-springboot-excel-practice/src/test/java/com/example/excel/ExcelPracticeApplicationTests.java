package com.example.excel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring 上下文加载冒烟测试。
 */
@SpringBootTest
class ExcelPracticeApplicationTests {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
