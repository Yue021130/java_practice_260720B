package com.example.sbcore.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BookRepository {

    private final Map<String, String> store = new HashMap<>();
    private final AtomicInteger callCount = new AtomicInteger(0);

    public BookRepository() {
        store.put("ISBN-001", "深入理解 Java 虚拟机");
        store.put("ISBN-002", "Spring 实战");
        store.put("ISBN-003", "Redis 设计与实现");
        store.put("ISBN-004", "Effective Java");
        store.put("ISBN-005", "Clean Code");
    }

    public String findByIsbn(String isbn) {
        callCount.incrementAndGet();
        sleep(ThreadLocalRandom.current().nextInt(20, 60));
        return store.get(isbn);
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }

    public boolean exists(String isbn) {
        return store.containsKey(isbn);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
