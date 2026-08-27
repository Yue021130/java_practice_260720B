package com.example.caa;

import com.example.caa.annotation.Audit;
import com.example.caa.demo.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Repeatable 重复注解单元测试。
 */
@SpringBootTest
public class RepeatableAnnotationUnitTest {

    @Test
    void auditedOperationShouldHaveTwoAuditAnnotations() throws NoSuchMethodException {
        Method method = DemoService.class.getMethod("auditedOperation");
        Audit[] audits = method.getAnnotationsByType(Audit.class);

        assertThat(audits).hasSize(2);
        assertThat(audits[0].action()).isEqualTo("CREATE_USER");
        assertThat(audits[1].action()).isEqualTo("SEND_MESSAGE");
    }
}
