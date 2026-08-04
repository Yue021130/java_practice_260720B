package com.example.sbcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookService {

    private int invocationCount = 0;

    @Cacheable(value = "books", key = "#isbn", condition = "#isbn.startsWith('ISBN')", unless = "#result == null")
    public String getBookTitle(String isbn) {
        invocationCount++;
        log.info("实际查询数据库，isbn={}", isbn);
        if ("ISBN-001".equals(isbn)) return "深入理解 Java 虚拟机";
        if ("ISBN-002".equals(isbn)) return "Spring 实战";
        if ("ISBN-003".equals(isbn)) return "Redis 设计与实现";
        return null;
    }

    public int getInvocationCount() {
        return invocationCount;
    }

    public void resetInvocationCount() {
        invocationCount = 0;
    }
}
