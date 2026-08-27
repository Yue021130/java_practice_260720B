package com.example.nf.dto;

import lombok.Data;

import java.util.List;

/**
 * 多行写入请求。
 */
@Data
public class LinesRequest {

    /** 相对沙箱的文件路径。 */
    private String path;

    /** 要写入的多行文本。 */
    private List<String> lines;
}
