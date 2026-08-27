package com.example.nf.dto;

import lombok.Data;

/**
 * 文本写入请求。
 */
@Data
public class WriteRequest {

    /** 相对沙箱的文件路径。 */
    private String path;

    /** 要写入的文本内容，UTF-8 编码。 */
    private String content;
}
