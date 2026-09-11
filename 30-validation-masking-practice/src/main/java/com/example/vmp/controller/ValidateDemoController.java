package com.example.vmp.controller;

import com.example.vmp.common.Result;
import com.example.vmp.validation.annotation.Phone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 参数校验演示接口：演示 @RequestParam 上的自定义注解与叠加校验。
 */
@Tag(name = "校验演示", description = "方法参数校验示例")
@Validated
@RestController
@RequestMapping("/api/validate")
public class ValidateDemoController {

    @Operation(summary = "@Phone 校验演示")
    @GetMapping("/phone")
    public Result<String> phone(@Phone @RequestParam String phone) {
        return Result.ok("手机号合法：" + phone);
    }

    @Operation(summary = "@NotBlank+@Size 叠加演示")
    @GetMapping("/keyword")
    public Result<String> keyword(@NotBlank(message = "keyword 不能为空")
                                  @Size(max = 10, message = "keyword 不能超过 10 个字符")
                                  @RequestParam String keyword) {
        return Result.ok("keyword 合法：" + keyword);
    }
}
