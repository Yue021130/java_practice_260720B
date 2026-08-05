package com.example.exception.spring;

import com.example.exception.hierarchy.BusinessException;
import com.example.exception.hierarchy.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Spring / Web 异常处理场景服务。
 */
@Service
public class SpringScenarioService {

    /**
     * 触发业务异常，由 @ControllerAdvice 统一处理。
     */
    public Map<String, Object> businessError() {
        throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单当前状态不允许支付");
    }

    /**
     * 业务错误码设计示例。
     */
    public Map<String, Object> errorCodeDesign() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rule", "错误码通常分段：1 位系统 + 3 位模块 + 3 位错误序号");
        result.put("example", "500001 = 5 后端系统 + 000 订单模块 + 001 状态错误");
        result.put("internationalization", "错误码映射到 message key，按 locale 读取不同语言文案");
        return result;
    }

    /**
     * 触发 ResponseStatusException。
     */
    public Map<String, Object> responseStatus() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有权限访问该资源");
    }

    /**
     * 触发未知异常，进入兜底 handler。
     */
    public Map<String, Object> unknownError() {
        throw new RuntimeException("模拟未知系统异常");
    }

    /**
     * 参数校验成功后的处理。
     */
    public Map<String, Object> validationSuccess(UserCreateRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", request.getUsername());
        result.put("email", request.getEmail());
        result.put("message", "参数校验通过");
        return result;
    }

    /**
     * 演示 @ExceptionHandler 优先级：精确匹配优先于父类匹配。
     */
    public Map<String, Object> handlerPriority(String type) {
        switch (type) {
            case "illegal-argument":
                throw new IllegalArgumentException("参数非法");
            case "illegal-state":
                throw new IllegalStateException("状态非法");
            default:
                throw new RuntimeException("未知异常");
        }
    }
}
