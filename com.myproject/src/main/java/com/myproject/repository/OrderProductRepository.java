package com.myproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myproject.entity.OrderProduct;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    // Custom query methods, if needed
}
