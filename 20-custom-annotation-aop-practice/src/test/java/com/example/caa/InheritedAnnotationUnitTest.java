package com.example.caa;

import com.example.caa.annotation.InheritedMarker;
import com.example.caa.inherited.BaseAnnotatedService;
import com.example.caa.inherited.InheritedChildService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Inherited 元注解单元测试。
 */
@SpringBootTest
public class InheritedAnnotationUnitTest {

    @Test
    void childClassShouldInheritAnnotationFromParent() {
        assertThat(BaseAnnotatedService.class.isAnnotationPresent(InheritedMarker.class)).isTrue();
        assertThat(InheritedChildService.class.isAnnotationPresent(InheritedMarker.class)).isTrue();

        InheritedMarker marker = InheritedChildService.class.getAnnotation(InheritedMarker.class);
        assertThat(marker).isNotNull();
        assertThat(marker.value()).isEqualTo("from-parent");
    }
}
