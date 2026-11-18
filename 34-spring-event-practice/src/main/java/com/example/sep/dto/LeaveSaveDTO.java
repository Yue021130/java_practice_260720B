package com.example.sep.dto;

import lombok.Data;

import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 请假提交入参。
 */
@Data
public class LeaveSaveDTO {

    /** 请假人用户名 */
    @NotBlank(message = "请假人不能为空")
    private String username;

    /** 请假类型：SICK / CASUAL / ANNUAL */
    @NotBlank(message = "请假类型不能为空")
    @Pattern(regexp = "^(SICK|CASUAL|ANNUAL)$", message = "请假类型只能是 SICK / CASUAL / ANNUAL")
    private String type;

    /** 请假事由 */
    @NotBlank(message = "请假事由不能为空")
    @Size(max = 200, message = "请假事由不能超过 200 字")
    private String reason;

    /** 开始日期（不能早于今天） */
    @NotNull(message = "开始日期不能为空")
    @FutureOrPresent(message = "开始日期不能早于今天")
    private LocalDate startDate;

    /** 结束日期（不能早于今天） */
    @NotNull(message = "结束日期不能为空")
    @FutureOrPresent(message = "结束日期不能早于今天")
    private LocalDate endDate;
}
