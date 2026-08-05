package com.example.exception.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * 异常进阶特性相关场景服务。
 */
@Slf4j
@Service
public class AdvancedScenarioService {

    /**
     * Java 7 多 catch。
     */
    public Map<String, Object> multiCatch() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        try {
            throwRandomException();
        } catch (IOException | SQLException e) {
            // 多 catch 中的异常不能是父子关系
            tips.add("多 catch 捕获: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        result.put("tips", tips);
        result.put("note", "多 catch 是语法糖，编译后生成多个 catch 块；catch 中的异常不能有继承关系");
        return result;
    }

    private void throwRandomException() throws IOException, SQLException {
        if (System.currentTimeMillis() % 2 == 0) {
            throw new IOException("IO 错误");
        } else {
            throw new SQLException("SQL 错误");
        }
    }

    /**
     * Java 7 更精确重抛：编译器能推断实际抛出的异常类型。
     */
    public Map<String, Object> rethrow() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            throwRandomException();
        } catch (Exception e) {
            // Java 7 之前这里必须声明 throws Exception
            // Java 7+ 编译器推断实际抛出 IOException 或 SQLException
            result.put("caughtType", e.getClass().getName());
            result.put("message", e.getMessage());
            result.put("note", "Java 7+ catch(Exception e) { throw e; } 编译器会保持原异常类型，无需声明 throws Exception");
        }
        return result;
    }

    /**
     * Lambda 中处理 checked exception 的三种方式。
     */
    public Map<String, Object> lambdaChecked() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        List<String> urls = Arrays.asList("http://ok.com", "bad");

        // 方式 1：在 lambda 内部 try-catch
        urls.forEach(url -> {
            try {
                checkUrl(url);
            } catch (IOException e) {
                tips.add("方式 1 捕获: " + url + " -> " + e.getMessage());
            }
        });

        // 方式 2：自定义包装方法把 checked 转 unchecked
        urls.forEach(url -> {
            try {
                checkUrlWrapped(url);
                tips.add("方式 2 成功: " + url);
            } catch (RuntimeException e) {
                tips.add("方式 2 捕获: " + url + " -> " + e.getMessage());
            }
        });

        // 方式 3：自定义函数式接口 CheckedConsumer
        urls.forEach(url -> {
            try {
                wrap((CheckedConsumer<String>) u -> {
                    checkUrl(u);
                    tips.add("方式 3 成功: " + u);
                }).accept(url);
            } catch (RuntimeException e) {
                tips.add("方式 3 捕获: " + url + " -> " + e.getMessage());
            }
        });

        result.put("tips", tips);
        result.put("summary", "Lambda 默认不支持 checked exception；可选：内部 try-catch / 包装为 unchecked / 自定义函数式接口");
        return result;
    }

    private void checkUrl(String url) throws IOException {
        if (!url.startsWith("http")) {
            throw new IOException("非法 URL: " + url);
        }
    }

    private void checkUrlWrapped(String url) {
        try {
            checkUrl(url);
        } catch (IOException e) {
            throw new RuntimeException("包装后抛出: " + e.getMessage(), e);
        }
    }

    private <T> Consumer<T> wrap(CheckedConsumer<T> checked) {
        return t -> {
            try {
                checked.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Stream 中的异常处理。
     */
    public Map<String, Object> streamException() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        List<String> inputs = Arrays.asList("1", "2", "abc", "4");

        // 错误写法：在 map 里抛 checked exception 编译不过
        // 正确写法：在 map 里捕获并包装为 RuntimeException，或提前过滤
        try {
            inputs.stream()
                    .map(this::parseOrThrow)
                    .forEach(i -> log.info("parsed: {}", i));
        } catch (RuntimeException e) {
            tips.add("Stream 中异常会短路，后续元素不再处理：" + e.getMessage());
        }

        // 更优雅：提取安全方法
        List<Integer> parsed = new ArrayList<>();
        inputs.forEach(s -> {
            try {
                parsed.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                tips.add("forEach 内单独捕获：跳过非法值 " + s);
            }
        });

        result.put("parsed", parsed);
        result.put("tips", tips);
        return result;
    }

    private int parseOrThrow(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException("解析失败: " + s, e);
        }
    }

    /**
     * Suppressed Exception：try-with-resources 的挂起异常。
     */
    public Map<String, Object> suppressed() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        try (FailingResource r = new FailingResource()) {
            r.doWork();
        } catch (RuntimeException e) {
            result.put("primaryMessage", e.getMessage());
            List<String> suppressed = new ArrayList<>();
            for (Throwable t : e.getSuppressed()) {
                suppressed.add(t.getClass().getName() + ": " + t.getMessage());
            }
            result.put("suppressed", suppressed);
        }

        return result;
    }

    /**
     * 异常屏蔽：catch 中抛新异常导致原异常丢失。
     */
    public Map<String, Object> exceptionMasking() {
        Map<String, Object> result = new LinkedHashMap<>();

        RuntimeException original = null;
        try {
            try {
                throw new RuntimeException("原始异常");
            } catch (RuntimeException e) {
                throw new RuntimeException("新异常"); // 原始异常丢失！
            }
        } catch (RuntimeException e) {
            result.put("message", e.getMessage());
            result.put("hasCause", e.getCause() != null);
            original = e;
        }

        // 正确做法
        try {
            try {
                throw new RuntimeException("原始异常");
            } catch (RuntimeException e) {
                throw new RuntimeException("新异常", e); // 保留 cause
            }
        } catch (RuntimeException e) {
            result.put("correctMessage", e.getMessage());
            result.put("correctHasCause", e.getCause() != null);
        }

        result.put("tip", "抛新异常时务必传入 cause，否则排障困难");
        return result;
    }

    /**
     * 异常性能开销：创建异常对象需要填充栈跟踪。
     */
    public Map<String, Object> performance() {
        Map<String, Object> result = new LinkedHashMap<>();

        long start1 = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            new RuntimeException("fast");
        }
        long normalCost = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            new RuntimeException("fast") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this; // 禁用栈跟踪，极快
                }
            };
        }
        long noStackCost = System.nanoTime() - start2;

        result.put("normalExceptionCostNs", normalCost);
        result.put("noStackTraceCostNs", noStackCost);
        result.put("tip", "异常创建开销主要在 fillInStackTrace；高吞吐场景可重写该方法，但会丢失调试信息");
        return result;
    }

    /**
     * 自定义 SQLException 占位类，避免与 java.sql 包同名冲突演示。
     */
    static class SQLException extends Exception {
        SQLException(String message) { super(message); }
    }

    /**
     * 教学用会失败的资源。
     */
    static class FailingResource implements AutoCloseable {
        void doWork() {
            throw new RuntimeException("业务执行失败");
        }

        @Override
        public void close() throws Exception {
            throw new Exception("关闭资源失败");
        }
    }
}
