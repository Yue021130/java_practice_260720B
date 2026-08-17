package com.example.os;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional + Stream 核心行为单元测试。
 */
public class OptionalStreamUnitTest {

    @Test
    void orElseShouldAlwaysEvaluateDefault() {
        // orElse 会立即求值，无论 Optional 是否有值。
        AtomicInteger counter = new AtomicInteger(0);
        String result = Optional.of("present").orElse(expensiveDefault(counter));

        assertThat(result).isEqualTo("present");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void orElseGetShouldLazyEvaluateDefault() {
        // orElseGet 是惰性求值，Optional 有值时不执行默认值函数。
        AtomicInteger counter = new AtomicInteger(0);
        String result = Optional.of("present").orElseGet(() -> expensiveDefault(counter));

        assertThat(result).isEqualTo("present");
        assertThat(counter.get()).isZero();
    }

    @Test
    void optionalChainShouldDefendNull() {
        String raw = "  Alice  ";
        String greeting = Optional.ofNullable(raw)
                .map(String::trim)
                .filter(s -> s.length() > 3)
                .map(String::toUpperCase)
                .map(s -> "Hello, " + s)
                .orElse("Guest");

        assertThat(greeting).isEqualTo("Hello, ALICE");

        String nullGreeting = Optional.ofNullable((String) null)
                .map(String::trim)
                .orElse("Guest");
        assertThat(nullGreeting).isEqualTo("Guest");
    }

    @Test
    void streamWithOptionalShouldHandleEmptyCollection() {
        List<String> emptyList = Collections.emptyList();

        // 空集合 + Optional 安全解包，不会 NPE。
        List<String> result = Optional.ofNullable(emptyList)
                .filter(list -> !list.isEmpty())
                .map(list -> list.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    void flatMapOptionalInJava8() {
        // Java 8 没有 Optional.stream()，用 flatMap 把 Optional<T> 展平成 Stream<T>。
        List<String> names = Arrays.asList("Alice", null, "Bob", "");
        List<String> nonBlank = names.stream()
                .map(n -> Optional.ofNullable(n).filter(s -> !s.trim().isEmpty()))
                .flatMap(opt -> opt.isPresent() ? java.util.stream.Stream.of(opt.get()) : java.util.stream.Stream.empty())
                .collect(Collectors.toList());

        assertThat(nonBlank).containsExactly("Alice", "Bob");
    }

    private String expensiveDefault(AtomicInteger counter) {
        counter.incrementAndGet();
        return "default";
    }
}
