package com.example.exception;

import com.example.exception.basics.BasicsScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常基础语法行为验证。
 */
@SpringBootTest
class BasicsBehaviorTest {

    @Autowired
    private BasicsScenarioService service;

    @Test
    void finallyShouldOverrideReturn() {
        Map<String, Object> result = service.finallyOverride(true);
        assertThat(result.get("result")).isEqualTo("finally-return");
    }

    @Test
    void exceptionChainShouldPreserveCause() {
        Map<String, Object> result = service.exceptionChain();
        assertThat(result.get("outerMessage")).isEqualTo("调用第三方服务失败");
        assertThat(result.get("causeType")).isEqualTo("java.io.IOException");
        assertThat(result.get("causeMessage")).isEqualTo("连接 reset by peer");
    }

    @Test
    void tryWithResourcesShouldAddSuppressed() throws Exception {
        Map<String, Object> result = service.tryWithResources(false, true);
        assertThat(result).containsKey("suppressed");
        @SuppressWarnings("unchecked")
        java.util.List<String> suppressed = (java.util.List<String>) result.get("suppressed");
        assertThat(suppressed).isNotEmpty();
        assertThat(suppressed).anyMatch(s -> s.contains("关闭失败"));
    }
}
