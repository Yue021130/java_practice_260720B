package com.example.caa;

import com.example.caa.builtin.BuiltinAnnotationDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内置注解单元测试。
 */
@SpringBootTest
public class BuiltinAnnotationUnitTest {

    @Autowired
    private BuiltinAnnotationDemo builtinAnnotationDemo;

    @Test
    void overrideShouldWorkAtCompileTime() {
        // @Override 是 SOURCE 保留策略，运行期无法反射读取，
        // 这里验证方法确实覆盖了 Object.toString() 的行为。
        assertThat(builtinAnnotationDemo.toString()).contains("正确覆盖了 toString()");
    }

    @Test
    void deprecatedShouldBePresentOnOldMethod() throws NoSuchMethodException {
        Method oldMethod = BuiltinAnnotationDemo.class.getMethod("oldMethod");
        // @Deprecated 是 RUNTIME 保留策略，可以通过反射读取
        assertThat(oldMethod.isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(builtinAnnotationDemo.oldMethod()).isEqualTo("这是老方法，不建议使用");
    }

    @Test
    void suppressWarningsShouldWork() {
        // @SuppressWarnings 是 SOURCE 保留策略，运行期不可见，
        // 这里验证被抑制警告的方法在运行期行为正确。
        List<String> list = builtinAnnotationDemo.suppressWarningDemo();
        assertThat(list).containsExactly("hello");
    }
}
