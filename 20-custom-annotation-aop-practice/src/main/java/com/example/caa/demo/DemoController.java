package com.example.caa.demo;

import com.example.caa.annotation.Audit;
import com.example.caa.common.ApiResponse;
import com.example.caa.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示控制器：提供前端可交互的 AOP 实验接口。
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Tag(name = "自定义注解 + AOP 演示", description = "日志 / 权限 / 限流 / 脱敏 / 耗时 / 组合注解")
public class DemoController {

    private final DemoService demoService;

    @GetMapping("/log")
    @Operation(summary = "操作日志示例", description = "触发 @LogOperation，观察后端日志")
    public ApiResponse<User> log(
            @Parameter(description = "用户 ID", example = "1")
            @RequestParam Long id) {
        return ApiResponse.ok(demoService.getUser(id));
    }

    @GetMapping("/permission/admin")
    @Operation(summary = "admin 权限示例", description = "需要 X-Role=admin 才能访问")
    public ApiResponse<Map<String, Object>> adminOnly(
            @Parameter(description = "角色头", example = "admin")
            @RequestHeader(value = "X-Role", defaultValue = "anonymous") String role) {
        return ApiResponse.ok(demoService.adminOnly());
    }

    @GetMapping("/permission/user")
    @Operation(summary = "user:view 权限示例", description = "需要 X-Role=admin 或 user 才能访问")
    public ApiResponse<Map<String, Object>> userView(
            @Parameter(description = "角色头", example = "user")
            @RequestHeader(value = "X-Role", defaultValue = "anonymous") String role) {
        return ApiResponse.ok(demoService.userView());
    }

    @GetMapping("/rate-limit")
    @Operation(summary = "接口限流示例", description = "1 秒内最多 2 次请求，快速点击会触发限流")
    public ApiResponse<Map<String, Object>> rateLimit() {
        return ApiResponse.ok(demoService.rateLimit());
    }

    @GetMapping("/masking")
    @Operation(summary = "数据脱敏示例", description = "返回用户对象，phone/email/idCard 会被脱敏")
    public ApiResponse<User> masking() {
        return ApiResponse.ok(demoService.maskingUser());
    }

    @GetMapping("/masking-list")
    @Operation(summary = "数据脱敏列表示例", description = "返回用户列表，phone/email 会被脱敏")
    public ApiResponse<List<User>> maskingList() {
        return ApiResponse.ok(demoService.maskingUserList());
    }

    @GetMapping("/timing")
    @Operation(summary = "耗时监控示例", description = "触发 @Timing，方法执行耗时会被记录")
    public ApiResponse<Map<String, Object>> timing() {
        return ApiResponse.ok(demoService.timing());
    }

    @GetMapping("/combine")
    @Operation(summary = "注解组合示例", description = "同时叠加 @LogOperation / @RequirePermission / @RateLimit / @Timing")
    public ApiResponse<Map<String, Object>> combine(
            @Parameter(description = "角色头", example = "admin")
            @RequestHeader(value = "X-Role", defaultValue = "anonymous") String role) {
        return ApiResponse.ok(demoService.combine());
    }

    @GetMapping("/error-log")
    @Operation(summary = "异常日志示例", description = "方法抛异常，观察 @LogOperation 记录的失败日志")
    public ApiResponse<Map<String, Object>> errorLog() {
        return ApiResponse.ok(demoService.errorLog());
    }

    @GetMapping("/audit")
    @Operation(summary = "重复注解示例", description = "读取方法上的多个 @Audit 注解")
    public ApiResponse<Map<String, Object>> audit() throws NoSuchMethodException {
        // 通过反射读取重复注解，不使用 AOP 也能展示 @Repeatable 的能力
        Audit[] audits = DemoService.class.getMethod("auditedOperation")
                .getAnnotationsByType(Audit.class);

        java.util.List<Map<String, String>> auditList = new java.util.ArrayList<>();
        for (Audit audit : audits) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("action", audit.action());
            item.put("desc", audit.desc());
            auditList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", audits.length);
        result.put("audits", auditList);
        result.put("tip", "Java 8 起可用 @Repeatable 让同一个注解在同一个位置出现多次");
        return ApiResponse.ok(result);
    }

    @PostMapping("/validate")
    @Operation(summary = "参数校验示例", description = "@Valid 触发 Bean Validation，校验失败返回 400")
    public ApiResponse<Map<String, Object>> validate(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "校验通过");
        result.put("userName", user.getName());
        return ApiResponse.ok(result);
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本专题核心考点与坑点清单")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topic", "自定义注解 + AOP 高阶玩法");
        result.put("annotations", new String[]{
                "@LogOperation：操作日志",
                "@RequirePermission：权限校验",
                "@RateLimit：接口限流",
                "@DataMasking：数据脱敏",
                "@Timing：耗时监控"
        });
        result.put("adviceTypes", new String[]{
                "@Before：方法执行前",
                "@After：方法执行后（无论是否异常）",
                "@AfterReturning：方法正常返回后",
                "@AfterThrowing：方法抛出异常后",
                "@Around：环绕通知，最强大的通知类型"
        });
        result.put("commonPitfalls", new String[]{
                "同类内部方法调用导致切面失效（自调用问题）",
                "AOP 代理后 this 不是原对象",
                "@Around 忘记调用 proceed() 导致方法不执行",
                "切面里抛异常要考虑全局异常处理",
                "生产环境限流不要用内存计数器，应用 Redis/Sentinel"
        });
        result.put("bestPractices", new String[]{
                "注解只做声明，逻辑交给切面",
                "切面要单一职责，不要一个切面干所有事",
                "日志/监控类切面不要阻塞主流程",
                "权限校验用 @Before 或 @Around，尽早拒绝",
                "注意 JDK 动态代理 vs CGLIB 代理的区别"
        });
        return ApiResponse.ok(result);
    }
}
