package com.example.mp.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Order 演示：第三个执行的启动器（未标注 @Order 默认 Integer.MAX_VALUE，最后执行）。
 */
@Slf4j
@Component
public class ThirdRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        String msg = "未标注 @Order ThirdRunner 执行（默认最后）";
        log.info(msg);
        StartupOrderRecorder.record(msg);
    }
}
