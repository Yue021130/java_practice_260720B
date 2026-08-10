package com.example.excel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.excel.config.ExcelPracticeProperties;

/**
 * Spring Boot + EasyExcel 导入导出实践启动类。
 *
 * - EasyExcel：阿里巴巴开源的 Excel 框架，注解驱动、流式读写、低内存占用
 * - 全部场景开箱即用：不依赖任何外部服务，导入演示在内存中生成样本文件再解析
 */
@SpringBootApplication
@EnableConfigurationProperties(ExcelPracticeProperties.class)
public class ExcelPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelPracticeApplication.class, args);
    }
}
