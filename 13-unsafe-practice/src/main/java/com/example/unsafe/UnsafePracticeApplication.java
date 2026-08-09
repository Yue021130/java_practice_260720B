package com.example.unsafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 魔法类 Unsafe 实践启动类。
 *
 * 本专题所有实验都围绕 {@code sun.misc.Unsafe}（俗称“魔法类”）展开：
 * 堆外内存 / 绕过构造器 / CAS / 字段偏移与对象布局 / park-unpark / 内存屏障 / 危险与本质。
 */
@SpringBootApplication
public class UnsafePracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnsafePracticeApplication.class, args);
    }
}
