package com.example.ae.repository;

import com.example.ae.entity.NotifyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知日志数据访问层。
 */
@Repository
public interface NotifyLogRepository extends JpaRepository<NotifyLog, Long> {

    List<NotifyLog> findByOrderNo(String orderNo);
}
