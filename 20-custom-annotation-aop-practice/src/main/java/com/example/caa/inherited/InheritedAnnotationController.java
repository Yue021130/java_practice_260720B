package com.example.caa.inherited;

import com.example.caa.annotation.InheritedMarker;
import com.example.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Inherited 元注解演示接口。
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Tag(name = "@Inherited 元注解演示", description = "子类继承父类上的注解")
public class InheritedAnnotationController {

    private final InheritedChildService inheritedChildService;

    @GetMapping("/inherited")
    @Operation(summary = "@Inherited 示例", description = "验证子类是否继承了父类上的 @InheritedMarker")
    public ApiResponse<Map<String, Object>> inherited() {
        Map<String, Object> result = new LinkedHashMap<>();

        boolean parentHas = BaseAnnotatedService.class.isAnnotationPresent(InheritedMarker.class);
        boolean childHas = InheritedChildService.class.isAnnotationPresent(InheritedMarker.class);
        InheritedMarker marker = InheritedChildService.class.getAnnotation(InheritedMarker.class);

        result.put("parentHasAnnotation", parentHas);
        result.put("childHasAnnotation", childHas);
        result.put("annotationValue", marker != null ? marker.value() : null);
        result.put("tip", "@Inherited 只对类继承有效，对接口实现无效；且仅当子类自身没有同名注解时才会继承");
        return ApiResponse.ok(result);
    }
}
