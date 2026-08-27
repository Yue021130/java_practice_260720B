package com.example.nf.dto;

import lombok.Data;

/**
 * 删除请求。
 */
@Data
public class DeleteRequest {

    /** 相对沙箱的路径。 */
    private String path;

    /** 是否递归删除目录。 */
    private boolean recursive = false;
}
