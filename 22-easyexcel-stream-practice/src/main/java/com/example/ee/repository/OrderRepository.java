package com.example.ee.repository;

import com.example.ee.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 订单数据访问层。
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 分页查询订单，流式导出时按页拉取数据。
     */
    Page<Order> findAllByOrderByIdAsc(Pageable pageable);
}
