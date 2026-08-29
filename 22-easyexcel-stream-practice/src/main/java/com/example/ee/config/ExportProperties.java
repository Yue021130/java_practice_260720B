package com.example.ee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导出任务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "export")
public class ExportProperties {

    /** 异步导出文件落盘目录。 */
    private String workDir = "./excel-export-work";
}
