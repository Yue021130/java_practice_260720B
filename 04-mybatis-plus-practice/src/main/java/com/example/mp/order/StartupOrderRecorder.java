package com.example.mp.order;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录带有 @Order 的 CommandLineRunner 实际启动顺序。
 */
@Component
public class StartupOrderRecorder {

    private static final List<String> ORDERED_LOGS = Collections.synchronizedList(new ArrayList<>());

    public static void record(String log) {
        ORDERED_LOGS.add(log);
    }

    public static List<String> getLogs() {
        return new ArrayList<>(ORDERED_LOGS);
    }

    public static void clear() {
        ORDERED_LOGS.clear();
    }
}
