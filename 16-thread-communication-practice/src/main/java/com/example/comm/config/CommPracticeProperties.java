package com.example.comm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程通信实践自定义配置（前缀 comm.practice，见 application.yml）。
 *
 * 用 @EnableConfigurationProperties 注册为 Bean，各 Service 注入读取，
 * 让「线程数 / 任务耗时 / 队列容量」等演示参数可调，也方便测试里覆盖调小。
 */
@Data
@ConfigurationProperties(prefix = "comm.practice")
public class CommPracticeProperties {

    /** 演示线程池参数 */
    private Pool pool = new Pool();

    /** 模拟「单个任务耗时」（毫秒），越大越能看清阻塞/等待效果 */
    private long taskCostMs = 30;

    /** 演示并发默认线程数 */
    private int defaultWorkers = 4;

    /** 阻塞队列演示容量 */
    private int queueSize = 5;

    @Data
    public static class Pool {

        /** 固定线程池核心/最大线程数 */
        private int coreSize = 4;

        /** 固定线程池最大线程数 */
        private int maxSize = 8;

        /** 固定线程池阻塞队列容量 */
        private int queueCapacity = 100;
    }
}
