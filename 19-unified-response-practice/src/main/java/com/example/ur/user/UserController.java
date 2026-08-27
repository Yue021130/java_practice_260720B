package com.example.ur.user;

import com.example.ur.common.advice.IgnoreResultWrap;
import com.example.ur.common.result.BusinessException;
import com.example.ur.common.result.PageResult;
import com.example.ur.common.result.Result;
import com.example.ur.common.result.ResultCode;
import com.example.ur.common.result.ResultFactory;
import com.example.ur.domain.User;
import com.example.ur.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户接口：演示统一返回结果封装的各种场景。
 *
 * <p>大部分方法直接返回业务对象，由 {@link com.example.ur.common.advice.GlobalResponseAdvice}
 * 自动包装成 Result；部分方法演示手动包装、异常、String 返回值、文件下载等特例。</p>
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "统一返回结果封装实战示例")
@ApiResponse(
        responseCode = "200",
        description = "统一返回 Result 结构：{code, msg, data, timestamp}",
        content = @Content(schema = @Schema(implementation = Result.class))
)
public class UserController {

    private final UserService userService;

    /**
     * 查询单个用户：返回 UserVO，自动包装成 Result&lt;UserVO&gt;。
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询单个用户", description = "返回 UserVO，会被自动包装成 Result<UserVO>")
    public UserVO getById(
            @Parameter(description = "用户 ID", example = "101")
            @PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 查询单个用户并自定义成功提示：演示 ResultFactory.success(msg, data) 重载。
     *
     * <p>Controller 直接返回 Result，GlobalResponseAdvice 识别出已经是 Result 后不再重复包装。</p>
     */
    @GetMapping("/detail-with-msg/{id}")
    @Operation(summary = "查询单个用户（自定义成功提示）", description = "演示 ResultFactory.success(msg, data)，返回 Result<UserVO>")
    public Result<UserVO> detailWithMsg(
            @Parameter(description = "用户 ID", example = "101")
            @PathVariable Long id) {
        UserVO userVO = userService.getById(id);
        return ResultFactory.success("查询成功", userVO);
    }

    /**
     * 查询全部用户：返回 List，自动包装成 Result&lt;List&lt;UserVO&gt;&gt;。
     */
    @GetMapping("/list")
    @Operation(summary = "查询用户列表", description = "返回 List<UserVO>，会被自动包装成 Result<List<UserVO>>")
    public List<UserVO> list() {
        return userService.list();
    }

    /**
     * 分页查询：返回 PageResult，自动包装成 Result&lt;PageResult&lt;UserVO&gt;&gt;。
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "返回 PageResult<UserVO>，会被自动包装成 Result<PageResult<UserVO>>")
    public PageResult<UserVO> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小", example = "3")
            @RequestParam(defaultValue = "3") long pageSize) {
        return userService.page(pageNum, pageSize);
    }

    /**
     * 创建用户：@Valid 触发参数校验，校验失败由全局异常处理器返回 Result。
     */
    @PostMapping("/create")
    @Operation(summary = "创建用户", description = "@Valid 校验失败时返回 code=400 的统一错误")
    public UserVO create(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody User user) {
        return userService.create(user);
    }

    /**
     * 更新用户：用户不存在时抛 BusinessException，由全局异常处理器返回 Result。
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户", description = "用户不存在时抛业务异常，返回统一错误")
    public UserVO update(
            @Parameter(description = "用户信息", required = true)
            @RequestBody User user) {
        return userService.update(user);
    }

    /**
     * 删除用户。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除成功返回 true，自动包装成 Result<Boolean>")
    public boolean delete(
            @Parameter(description = "用户 ID", example = "101")
            @PathVariable Long id) {
        return userService.delete(id);
    }

    /**
     * 手动包装示例：Controller 直接返回 Result，GlobalResponseAdvice 不会重复包装。
     */
    @GetMapping("/manual-wrap/{id}")
    @Operation(summary = "手动包装示例", description = "Controller 直接返回 Result，验证不会重复包装")
    public Result<UserVO> manualWrap(
            @Parameter(description = "用户 ID", example = "101")
            @PathVariable Long id) {
        return userService.manualWrap(id);
    }

    /**
     * 主动抛业务异常：演示异常也走统一 Result。
     */
    @GetMapping("/not-found")
    @Operation(summary = "业务异常示例", description = "主动抛 BusinessException，验证异常统一处理")
    public UserVO notFound() {
        throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "模拟用户不存在");
    }

    /**
     * String 返回值推荐做法：直接返回 Result&lt;String&gt;。
     *
     * <p>原文第 8 节坑 2 指出，Controller 直接返回裸 String 时，StringHttpMessageConverter
     * 容易导致前端收到字符串 JSON。更稳妥的做法是不让 Controller 返回裸 String，
     * 而是直接返回 ResultFactory.success("已发送")，从源头避开这个坑。</p>
     */
    @GetMapping("/raw-string")
    @Operation(summary = "String 返回值推荐做法", description = "Controller 直接返回 Result<String>，不再依赖自动包装 String")
    public Result<String> rawString() {
        return ResultFactory.success("ok");
    }

    /**
     * String 返回值对比示例：用 @IgnoreResultWrap 跳过统一包装，直接返回纯文本。
     *
     * <p>展示另一种稳妥做法：如果业务场景必须返回裸字符串，就明确跳过统一包装，
     * 并自行控制 Content-Type，避免被 ResponseBodyAdvice 包一层 Result。</p>
     */
    @GetMapping("/raw-string-bare")
    @IgnoreResultWrap
    @Operation(summary = "String 跳过包装示例", description = "@IgnoreResultWrap + ResponseEntity<String> 直接返回纯文本")
    public ResponseEntity<String> rawStringBare() {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body("ok");
    }

    /**
     * 文件下载：标注 @IgnoreResultWrap，跳过统一包装，直接返回文件流。
     */
    @GetMapping("/download")
    @IgnoreResultWrap
    @Operation(summary = "文件下载示例", description = "标注 @IgnoreResultWrap，跳过统一包装，直接返回文件流")
    public ResponseEntity<byte[]> download() {
        String content = "name,age,email\n张三,28,zhangsan@example.com\n李四,35,lisi@example.com";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    /**
     * 八股速记。
     */
    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本专题的核心考点与常见坑点")
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topic", "Spring Boot 统一返回结果封装");
        result.put("coreComponents", new String[]{
                "Result<T>：code / msg / data / timestamp",
                "ResultCode：业务状态码枚举，禁止魔法数字",
                "ResultFactory：静态工厂方法，简化业务代码",
                "PageResult<T>：分页统一封装，字段名前端约定死",
                "GlobalResponseAdvice：ResponseBodyAdvice 全局自动包装",
                "@IgnoreResultWrap：跳过包装的特例注解",
                "GlobalExceptionHandler：任何异常都返回 Result",
                "BusinessException：携带 code 的业务异常"
        });
        result.put("commonPitfalls", new String[]{
                "Result 套 Result：beforeBodyWrite 里要先判断 body instanceof Result",
                "String 返回值变字符串 JSON：手动 Jackson 序列化或调整 Converter 顺序",
                "文件下载被包装成 JSON：用 @IgnoreResultWrap 跳过",
                "异常返回结构不一致：全局异常处理器兜底 Exception",
                "swagger 显示裸类型：文档里显式说明 data 真实类型"
        });
        result.put("bestPractices", new String[]{
                "成功码与前端约定唯一值（如 0）",
                "保留 timestamp 便于排查",
                "分页返回 VO 而不是裸实体，防止敏感字段泄露",
                "前端 axios 拦截器统一根据 code 处理成功/失败/登录过期"
        });
        return result;
    }
}
