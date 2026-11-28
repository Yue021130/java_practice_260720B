package com.example.prod.config;

import com.example.prod.entity.AuditRecord;
import com.example.prod.mapper.AuditRecordMapper;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计事件落库实现：把 Actuator 的 AuditEventRepository 桥接到 MySQL。
 *
 * <p>Spring Boot 的审计框架天然集成在 Actuator 里——任何地方
 * 调用 repository.add(new AuditEvent(principal, type, data)) 即完成
 * 记录。默认 InMemoryAuditEventRepository 重启即丢且不可查，
 * 生产必须替换为持久化实现（本类）。</p>
 *
 * <p>合规要点（八股）：审计日志 append-only——本表只允许 INSERT 和
 * SELECT，不提供 UPDATE/DELETE 接口；定期归档并做完整性校验，
 * 防篡改是等保审计项的硬要求。</p>
 */
@Component
public class DbAuditEventRepository implements AuditEventRepository {

    private final AuditRecordMapper auditRecordMapper;

    public DbAuditEventRepository(AuditRecordMapper auditRecordMapper) {
        this.auditRecordMapper = auditRecordMapper;
    }

    @Override
    public void add(AuditEvent event) {
        AuditRecord record = new AuditRecord();
        record.setPrincipal(event.getPrincipal());
        record.setType(event.getType());
        record.setData(String.valueOf(event.getData()));
        record.setTimestamp(LocalDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault()));
        auditRecordMapper.insert(record);
    }

    /** 按时间倒序查询（AuditRecord 的 timestamp 为 LocalDateTime，见实体类注释） */
    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        // 演示工程简化为返回空列表（页面查询走 AuditController 直接分页查表）；
        // 生产在此按条件构造查询并回填 AuditEvent
        return new ArrayList<>();
    }
}
