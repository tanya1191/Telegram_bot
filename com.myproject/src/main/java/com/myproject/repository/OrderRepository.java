package com.myproject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.myproject.entity.Order;

//OrderRepository.java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
 List<Order> findByChatId(Long chatId);
 // Add other custom queries if needed
}
