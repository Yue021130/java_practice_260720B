package com.example.nf.dto;

import lombok.Data;

import java.util.Map;

/**
 * Properties 文件写入请求。
 */
@Data
public class PropertiesWriteRequest {

    /** 相对沙箱的文件路径。 */
    private String path;

    /** 键值对属性。 */
    private Map<String, String> properties;
}
