package com.example.eep.repository;

import com.example.eep.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品数据访问层。
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
