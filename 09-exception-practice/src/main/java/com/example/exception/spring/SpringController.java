package com.example.exception.spring;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * Spring / Web 异常处理实验接口。
 */
@RestController
@RequestMapping("/api/spring")
@RequiredArgsConstructor
@Validated
@Tag(name = "05. Spring 全局异常处理", description = "@ControllerAdvice、参数校验、业务错误码、ResponseStatusException")
public class SpringController {

    private final SpringScenarioService service;

    @PostMapping("/business-error")
    @Operation(summary = "业务异常", description = "抛 BusinessException，由全局处理器返回错误码")
    public ApiResponse<Map<String, Object>> businessError() {
        return ApiResponse.success(service.businessError());
    }

    @GetMapping("/error-code")
    @Operation(summary = "业务错误码设计", description = "错误码分段规则与国际化思路")
    public ApiResponse<Map<String, Object>> errorCode() {
        return ApiResponse.success(service.errorCodeDesign());
    }

    @PostMapping("/validation")
    @Operation(summary = "参数校验异常", description = "@RequestBody 校验失败返回 MethodArgumentNotValidException")
    public ApiResponse<Map<String, Object>> validation(
            @RequestBody @Validated UserCreateRequest request) {
        return ApiResponse.success(service.validationSuccess(request));
    }

    @GetMapping("/validation-param")
    @Operation(summary = "参数校验异常（@RequestParam）", description = "@RequestParam 校验失败返回 ConstraintViolationException")
    public ApiResponse<String> validationParam(
            @NotBlank(message = "name 不能为空")
            @Size(min = 2, max = 10, message = "name 长度 2-10")
            @RequestParam String name) {
        return ApiResponse.success("hello " + name);
    }

    @PostMapping("/response-status")
    @Operation(summary = "ResponseStatusException", description = "Spring 内置轻量异常")
    public ApiResponse<Map<String, Object>> responseStatus() {
        return ApiResponse.success(service.responseStatus());
    }

    @PostMapping("/unknown-error")
    @Operation(summary = "未知异常兜底", description = "进入 @ExceptionHandler(Exception.class) 统一处理")
    public ApiResponse<Map<String, Object>> unknownError() {
        return ApiResponse.success(service.unknownError());
    }

    @PostMapping("/handler-priority")
    @Operation(summary = "ExceptionHandler 优先级", description = "精确匹配优先于父类匹配")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlerPriority(
            @Parameter(description = "illegal-argument / illegal-state / other")
            @RequestParam(defaultValue = "illegal-argument") String type) {
        return ResponseEntity.ok(ApiResponse.success(service.handlerPriority(type)));
    }
}
