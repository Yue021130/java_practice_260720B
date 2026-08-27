package com.example.nf.dto;

import com.example.nf.service.NioFileService;
import lombok.Data;

/**
 * 创建文件/目录请求。
 */
@Data
public class CreateRequest {

    /** 相对沙箱的路径，为空表示在沙箱根目录创建临时文件/目录。 */
    private String path;

    /** 创建类型：FILE / DIRECTORY / TEMP_FILE / TEMP_DIR。 */
    private NioFileService.CreateType type = NioFileService.CreateType.FILE;

    /** 临时文件/目录前缀。 */
    private String prefix;

    /** 临时文件后缀。 */
    private String suffix;
}
