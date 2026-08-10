package com.example.excel.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Excel 实验中的业务异常。
 *
 * 已知问题（文件为空 / 表头不对 / 数据校验失败 / 解析失败等）都转成它，
 * 由全局异常处理器转成可读的 ApiResponse 返回，不让堆栈直接暴露给前端。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExcelBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private int code;

    public ExcelBizException(String message) {
        this(400100, message);
    }

    public ExcelBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ExcelBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }
}
