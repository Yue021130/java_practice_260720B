package com.example.mp.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Order 演示：第一个执行的启动器。
 */
@Slf4j
@Component
@Order(10)
public class FirstRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        String msg = "@Order(10) FirstRunner 执行";
        log.info(msg);
        StartupOrderRecorder.record(msg);
    }
}
