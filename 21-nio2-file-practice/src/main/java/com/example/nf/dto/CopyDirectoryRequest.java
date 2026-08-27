package com.example.nf.dto;

import lombok.Data;

/**
 * 目录复制请求。
 */
@Data
public class CopyDirectoryRequest {

    /** 源目录相对路径。 */
    private String src;

    /** 目标目录相对路径。 */
    private String dst;
}
