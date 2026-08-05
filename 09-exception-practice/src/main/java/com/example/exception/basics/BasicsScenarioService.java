package com.example.exception.basics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 异常基础语法相关场景服务。
 */
@Slf4j
@Service
public class BasicsScenarioService {

    /**
     * 演示 try-catch-finally 执行顺序与 return 的相互作用。
     */
    public Map<String, Object> executionOrder(String scenario) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> steps = new ArrayList<>();

        switch (scenario) {
            case "normal":
                steps.addAll(normalFlow());
                result.put("description", "无异常：try → finally");
                break;
            case "catch":
                steps.addAll(catchFlow());
                result.put("description", "有异常被捕获：try → catch → finally");
                break;
            case "uncaught":
                try {
                    steps.addAll(uncaughtFlow());
                } catch (RuntimeException e) {
                    steps.add("外层 catch: " + e.getMessage());
                }
                result.put("description", "异常未被当前 catch 捕获：try → finally → 外层 catch");
                break;
            case "return":
                result.put("returnValue", returnInFinally());
                result.put("description", "finally 中的 return 会覆盖 try 中的 return");
                steps.add("见 returnValue");
                break;
            default:
                throw new IllegalArgumentException("不存在的场景: " + scenario);
        }

        result.put("steps", steps);
        return result;
    }

    private List<String> normalFlow() {
        List<String> steps = new ArrayList<>();
        try {
            steps.add("进入 try");
        } finally {
            steps.add("进入 finally");
        }
        steps.add("方法继续执行");
        return steps;
    }

    private List<String> catchFlow() {
        List<String> steps = new ArrayList<>();
        try {
            steps.add("进入 try");
            throw new RuntimeException("业务异常");
        } catch (RuntimeException e) {
            steps.add("进入 catch: " + e.getMessage());
        } finally {
            steps.add("进入 finally");
        }
        steps.add("方法继续执行");
        return steps;
    }

    private List<String> uncaughtFlow() {
        List<String> steps = new ArrayList<>();
        try {
            steps.add("进入 try");
            throw new IllegalStateException("状态异常");
        } catch (NullPointerException e) {
            steps.add("进入 catch（不会执行，因为类型不匹配）");
        } finally {
            steps.add("进入 finally");
        }
        steps.add("不会执行到这里");
        return steps;
    }

    /**
     * finally 里的 return 会覆盖 try 里的 return。
     */
    private String returnInFinally() {
        try {
            return "try-return";
        } finally {
            return "finally-return";
        }
    }

    /**
     * 演示 finally 覆盖 catch 中的 throw。
     */
    public Map<String, Object> finallyOverride(boolean withReturn) {
        Map<String, Object> result = new LinkedHashMap<>();
        String value;
        if (withReturn) {
            value = returnInFinally2();
            result.put("description", "finally 中的 return 把 catch 里的 throw 吞掉了");
        } else {
            value = throwInFinally();
            result.put("description", "finally 中的 throw 把 catch 里的 throw 覆盖了");
        }
        result.put("result", value);
        return result;
    }

    private String returnInFinally2() {
        try {
            throw new RuntimeException("try 异常");
        } catch (RuntimeException e) {
            throw new RuntimeException("catch 异常");
        } finally {
            return "finally-return";
        }
    }

    private String throwInFinally() {
        try {
            throw new RuntimeException("try 异常");
        } catch (RuntimeException e) {
            throw new RuntimeException("catch 异常");
        } finally {
            throw new RuntimeException("finally 异常");
        }
    }

    /**
     * 演示 try-with-resources。
     */
    public Map<String, Object> tryWithResources(boolean businessFail, boolean closeFail) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logs = new ArrayList<>();

        if (!closeFail) {
            try (ResourceA a = new ResourceA("资源A"); ResourceA b = new ResourceA("资源B")) {
                a.doSomething(businessFail);
            }
            result.put("description", "资源按打开逆序自动关闭：B → A");
        } else {
            try (ResourceWithCloseException r = new ResourceWithCloseException("会关闭失败的资源")) {
                r.doSomething();
            } catch (RuntimeException e) {
                List<String> suppressed = new ArrayList<>();
                for (Throwable t : e.getSuppressed()) {
                    suppressed.add(t.getClass().getName() + ": " + t.getMessage());
                }
                result.put("description", "close 抛出的异常被挂到业务异常的 suppressed 上");
                result.put("businessException", e.getMessage());
                result.put("suppressed", suppressed);
                result.put("logs", logs);
                return result;
            }
        }

        result.put("logs", logs);
        return result;
    }

    /**
     * 演示异常链：低层异常转换为高层业务异常时保留 cause。
     */
    public Map<String, Object> exceptionChain() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            callThirdParty();
        } catch (RuntimeException e) {
            result.put("outerMessage", e.getMessage());
            result.put("causeType", e.getCause().getClass().getName());
            result.put("causeMessage", e.getCause().getMessage());
            result.put("tip", "通过 e.getCause() 可以追溯到原始异常，便于排查");
        }
        return result;
    }

    private void callThirdParty() {
        try {
            lowLevelIo();
        } catch (java.io.IOException e) {
            // 包装异常，保留 cause
            throw new RuntimeException("调用第三方服务失败", e);
        }
    }

    private void lowLevelIo() throws java.io.IOException {
        throw new java.io.IOException("连接 reset by peer");
    }

    /**
     * 演示异常信息脱敏：内部日志记录完整异常，对外返回脱敏信息。
     */
    public Map<String, Object> maskSensitive() {
        Map<String, Object> result = new LinkedHashMap<>();
        String idCard = "11010119900101XXXX";
        try {
            validateIdCard(idCard);
        } catch (IllegalArgumentException e) {
            // 内部日志：记录完整信息（生产会打日志）
            log.error("业务校验失败，原始异常：", e);
            // 对外返回：脱敏
            result.put("publicMessage", "身份证号校验失败");
            result.put("publicCode", 400001);
            result.put("internalMessage", e.getMessage()); // 教学展示用，真实项目不返回
            result.put("tip", "生产环境应只返回 publicMessage，内部日志记录完整 stack trace");
        }
        return result;
    }

    private void validateIdCard(String idCard) {
        throw new IllegalArgumentException("身份证号 " + idCard + " 格式非法");
    }

    /**
     * finally 不执行的极端情况。
     */
    public Map<String, Object> finallyNotExecute() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cases", Arrays.asList(
                "1. 线程在 try 执行过程中被中断/杀死",
                "2. 调用 System.exit(0) 直接退出 JVM",
                "3. JVM 崩溃（如 SIGKILL、硬件故障）",
                "4. try 是无限循环，永远不会进入 finally"
        ));
        result.put("conclusion", "finally 在正常情况下几乎一定执行，但不能依赖它做生命安全相关的清理");
        return result;
    }
}
