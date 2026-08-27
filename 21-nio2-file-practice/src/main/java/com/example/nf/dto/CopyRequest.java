package com.example.nf.dto;

import lombok.Data;

/**
 * 文件复制请求。
 */
@Data
public class CopyRequest {

    /** 源文件相对路径。 */
    private String src;

    /** 目标文件相对路径。 */
    private String dst;

    /** 是否覆盖已存在目标。 */
    private boolean replaceExisting = true;

    /** 是否复制文件属性（修改时间等）。 */
    private boolean copyAttributes = false;
}
