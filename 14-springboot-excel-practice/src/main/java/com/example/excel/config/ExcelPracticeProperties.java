package com.example.excel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel 实践自定义配置（前缀 excel.practice，见 application.yml）。
 *
 * 用 @EnableConfigurationProperties 注册为 Bean，各 Service 注入读取，
 * 让「批量大小 / 分页行数」等演示参数可调，也方便测试里覆盖。
 */
@Data
@ConfigurationProperties(prefix = "excel.practice")
public class ExcelPracticeProperties {

    /** 大数据量导出时每页查询/写入的行数（真实工程一般 5000~20000） */
    private int bigdataPageSize = 5000;

    /** 导入监听器每攒够多少行批量落库一次 */
    private int batchSize = 100;

    /** 错误行回写时追加的错误列名 */
    private String errorColumnName = "错误信息";
}
