package com.example.exception.hierarchy;

import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * 异常体系与分类相关场景服务。
 */
@Service
public class HierarchyScenarioService {

    /**
     * 返回 Throwable 家谱信息。
     */
    public Map<String, Object> familyTree() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("root", "java.lang.Throwable");

        List<Map<String, Object>> children = new ArrayList<>();

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("name", "java.lang.Error");
        error.put("type", "unchecked");
        error.put("description", "严重系统错误，通常不应捕获处理");
        error.put("examples", Arrays.asList("OutOfMemoryError", "StackOverflowError", "NoClassDefFoundError"));
        children.add(error);

        Map<String, Object> exception = new LinkedHashMap<>();
        exception.put("name", "java.lang.Exception");
        exception.put("type", "checked-root");
        exception.put("description", "程序本身可以处理的异常基类");
        exception.put("examples", Arrays.asList("IOException", "SQLException", "ClassNotFoundException"));

        List<Map<String, Object>> exceptionChildren = new ArrayList<>();
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("name", "java.lang.RuntimeException");
        runtime.put("type", "unchecked");
        runtime.put("description", "运行时异常，编译器不强制处理");
        runtime.put("examples", Arrays.asList("NullPointerException", "ClassCastException",
                "IllegalArgumentException", "ConcurrentModificationException"));
        exceptionChildren.add(runtime);
        exception.put("children", exceptionChildren);
        children.add(exception);

        result.put("children", children);

        Map<String, String> compare = new LinkedHashMap<>();
        compare.put("checked", "编译期检查，调用方必须处理或继续 throws，如 IOException / SQLException");
        compare.put("unchecked", "运行期异常，继承 RuntimeException / Error，调用方不强制处理");
        result.put("checkedVsUnchecked", compare);

        return result;
    }

    /**
     * 演示 checked exception 与 unchecked exception 的差异。
     *
     * 为了接口安全，内部捕获异常并返回说明；真实编译期行为见代码注释。
     */
    public Map<String, Object> checkedUnchecked(boolean checked) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (checked) {
            try {
                // 模拟读取一个不存在的文件，方法签名必须声明 throws 或调用方 try-catch
                throw new FileNotFoundException("文件 /tmp/not-exist.txt 不存在（checked exception）");
            } catch (FileNotFoundException e) {
                result.put("type", "checked");
                result.put("exceptionClass", "java.io.FileNotFoundException");
                result.put("message", e.getMessage());
                result.put("note", "checked exception 必须显式处理或继续 throws");
            }
        } else {
            try {
                // 模拟除零，ArithmeticException 继承 RuntimeException，属于 unchecked
                int a = 1 / 0;
            } catch (ArithmeticException e) {
                result.put("type", "unchecked");
                result.put("exceptionClass", "java.lang.ArithmeticException");
                result.put("message", e.getMessage());
                result.put("note", "unchecked exception 不需要显式声明");
            }
        }
        return result;
    }

    /**
     * 演示自定义业务异常：携带错误码 + cause。
     */
    public Map<String, Object> customException(boolean throwWithCause) {
        if (throwWithCause) {
            try {
                someDataAccess();
            } catch (SQLException e) {
                // 低层 SQLException 转换为业务异常，保留 cause 便于排错
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, e);
            }
        }
        return Collections.singletonMap("message", "未抛异常，自定义 BusinessException 见代码");
    }

    private void someDataAccess() throws SQLException {
        throw new SQLException("数据库连接超时");
    }

    /**
     * 何时用 checked / unchecked 的建议。
     */
    public Map<String, Object> whenToUse() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", "调用方有能力且应该恢复的场景，如文件不存在可换路径重试、网络超时可重试");
        result.put("unchecked", "编程错误或服务端无法恢复的场景，如空指针、参数非法、状态不一致");
        result.put("bestPractice", "Java 社区更推荐 unchecked（Effective Java 观点）：减少样板代码，让调用方选择是否处理");
        return result;
    }
}
