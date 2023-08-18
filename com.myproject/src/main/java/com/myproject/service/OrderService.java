package com.myproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.myproject.entity.Order;
import com.myproject.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order findOrCreateOrderForChat(long chatId) {
        // Check if an order already exists for the given chatId
        Order existingOrder = (Order) orderRepository.findByChatId(chatId);

        if (existingOrder == null) {
            // If no order exists, create a new one
            existingOrder = new Order();
            existingOrder.setChatId(chatId);
            
            // You can set any default values or additional information for the order here
            // For example, you might want to set the initial order status or the order date/time

            // Save the new order to the database
            existingOrder = orderRepository.save(existingOrder);
        }

        return existingOrder;
    }
}
