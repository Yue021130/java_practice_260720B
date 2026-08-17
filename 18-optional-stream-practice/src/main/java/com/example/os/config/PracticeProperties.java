package com.example.os.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义业务配置：前缀 os.practice，方便在不改代码的情况下调整实验参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "os.practice")
public class PracticeProperties {

    /**
     * 默认统计日期范围（天）。
     */
    private int defaultDateRangeDays = 30;

    /**
     * 数据清洗单次最大处理条数。
     */
    private int maxCleanBatchSize = 1000;

    /**
     * 通知保留天数。
     */
    private int notificationKeepDays = 7;

    /**
     * Excel 导入允许的最大空值率（百分比）。
     */
    private int excelMaxEmptyRate = 20;
}
