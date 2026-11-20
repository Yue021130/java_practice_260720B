package com.example.epc.oplog;

import cn.dev33.satoken.stp.StpUtil;
import com.example.epc.entity.OpLog;
import com.example.epc.mapper.OpLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作日志切面：拦截 @OpLog 标注的方法，记录审计日志。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>审计**不能阻断业务**：记录过程 catch 所有异常，业务异常照常上抛；
 *   原方法抛异常时审计照常落库（记录 costMs 与异常消息），再重新抛出；</li>
 *   <li>参数摘要用 Jackson 序列化并截断，防超长参数撑爆日志表；</li>
 *   <li>IP 从 RequestContextHolder 取——@OpLog 只用于 Web 层。</li>
 * </ul>
 */
@Aspect
@Component
public class OpLogAspect {

    private static final int MAX_PARAMS_LENGTH = 2000;

    private final OpLogMapper opLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpLogAspect(OpLogMapper opLogMapper) {
        this.opLogMapper = opLogMapper;
    }

    @Around("@annotation(opLogAnn)")
    public Object around(ProceedingJoinPoint pjp, com.example.epc.oplog.OpLog opLogAnn) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            saveLog(pjp, opLogAnn, System.currentTimeMillis() - start, error);
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, com.example.epc.oplog.OpLog opLogAnn, long costMs, Throwable error) {
        try {
            OpLog log = new OpLog();
            log.setTitle(opLogAnn.value());
            log.setPerm(opLogAnn.perm());
            try {
                log.setOperator(String.valueOf(StpUtil.getLoginIdAsLong()));
            } catch (Exception e) {
                log.setOperator("anonymous");
            }
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            log.setMethod(signature.getDeclaringType().getSimpleName() + "#" + signature.getName());
            log.setIp(currentIp());
            log.setParams(truncate(serializeArgs(pjp.getArgs())));
            if (error != null) {
                log.setTitle(opLogAnn.value() + "（失败：" + error.getMessage() + "）");
            }
            log.setCostMs(costMs);
            log.setCreateTime(LocalDateTime.now());
            opLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计失败不阻断业务
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "-";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip.split(",")[0].trim();
        } catch (Exception e) {
            return "-";
        }
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_PARAMS_LENGTH ? s : s.substring(0, MAX_PARAMS_LENGTH);
    }
}
