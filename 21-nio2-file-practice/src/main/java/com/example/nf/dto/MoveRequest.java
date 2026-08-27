package com.example.nf.dto;

import lombok.Data;

/**
 * 移动/重命名请求。
 */
@Data
public class MoveRequest {

    /** 源路径。 */
    private String src;

    /** 目标路径。 */
    private String dst;

    /** 是否使用原子移动。 */
    private boolean atomic = false;
}
