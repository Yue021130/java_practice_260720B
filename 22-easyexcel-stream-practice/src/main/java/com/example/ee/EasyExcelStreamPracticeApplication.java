package com.example.ee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * EasyExcel 流式导出实战启动类。
 */
@EnableAsync
@SpringBootApplication
public class EasyExcelStreamPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyExcelStreamPracticeApplication.class, args);
    }
}
