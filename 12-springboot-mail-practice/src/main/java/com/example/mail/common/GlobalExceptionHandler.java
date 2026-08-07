package com.example.mail.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.mail.MessagingException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring 全局异常处理。
 *
 * 统一拦截 Controller 抛出的异常，转换为 ApiResponse，避免异常堆栈直接暴露给前端。
 * 邮件相关异常（MessagingException / MailException）统一返回 400 并给出可读提示。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 邮件业务异常：已知错误，返回错误码与提示。
     */
    @ExceptionHandler(MailBizException.class)
    public ResponseEntity<ApiResponse<Void>> handleMailBizException(MailBizException e) {
        log.warn("邮件业务异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 邮件协议异常：构造/解析 MimeMessage 失败。
     */
    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessagingException(MessagingException e) {
        log.warn("邮件协议异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400200, "邮件消息构造失败：" + e.getMessage()));
    }

    /**
     * 邮件发送异常：SMTP 连接失败、账号密码错误、收件人被拒等。
     */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiResponse<Void>> handleMailException(MailException e) {
        log.warn("邮件发送异常：{}", e.getMessage());
        String detail = e instanceof MailSendException ? "SMTP 服务器拒绝或网络异常" : "邮件发送失败";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400201, detail + "：" + e.getMessage()));
    }

    /**
     * 参数校验异常：@RequestBody 形式。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400001, "参数校验失败", errors));
    }

    /**
     * 兜底：未知异常，记录完整堆栈，对外脱敏。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常：", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500000, "系统繁忙，请稍后重试"));
    }
}
