package com.example.sl.optional;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OptionalService {

    public Map<String, Object> safeDemo() {
        Map<String, Object> result = new HashMap<>();

        String rawName = "  Alice  ";
        String nullName = null;

        String greeting1 = Optional.ofNullable(rawName)
                .map(String::trim)
                .map(String::toUpperCase)
                .map(n -> "Hello, " + n)
                .orElse("Hello, Guest");

        String greeting2 = Optional.ofNullable(nullName)
                .map(String::trim)
                .map(n -> "Hello, " + n)
                .orElse("Hello, Guest");

        String filtered = Optional.ofNullable(rawName)
                .map(String::trim)
                .filter(n -> n.length() > 3)
                .orElse("short");

        String orElseGet = Optional.ofNullable(nullName)
                .map(String::trim)
                .orElseGet(() -> {
                    System.out.println("orElseGet 被调用，产生默认值");
                    return "lazy-default";
                });

        List<String> sideEffects = new ArrayList<>();
        Optional.ofNullable(rawName)
                .map(String::trim)
                .ifPresent(n -> sideEffects.add("present: " + n));

        String orElseThrow = Optional.ofNullable(rawName)
                .map(String::trim)
                .orElseThrow(() -> new IllegalArgumentException("name cannot be null"));

        result.put("greeting1", greeting1);
        result.put("greeting2", greeting2);
        result.put("filtered", filtered);
        result.put("orElseGet", orElseGet);
        result.put("sideEffects", sideEffects);
        result.put("orElseThrow", orElseThrow);
        result.put("interviewNote", "Optional 用于明确表达可能为空，避免 NPE：ofNullable 包装、map/filter 链式处理、orElse/orElseGet/orElseThrow/ifPresent 安全消费；orElseGet 是惰性求值。");
        return result;
    }
}
