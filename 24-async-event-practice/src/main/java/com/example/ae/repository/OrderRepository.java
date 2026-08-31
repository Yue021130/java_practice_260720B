package com.example.ae.repository;

import com.example.ae.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 订单数据访问层。
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
