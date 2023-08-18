package com.myproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.myproject.repository.OrderProductRepository;
import com.myproject.repository.OrderRepository;
import com.myproject.repository.ProductRepository;
import com.myproject.service.OrderService;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		
		 ApplicationContext context = SpringApplication.run(Main.class, args);

	        // Retrieve the ProductRepository bean from the context
	        ProductRepository productRepository = context.getBean(ProductRepository.class);

	        // Retrieve the OrderRepository bean from the context
	        OrderRepository orderRepository = context.getBean(OrderRepository.class);
	        OrderProductRepository orderProductRepository = context.getBean(OrderProductRepository.class);

	        OrderService orderService = context.getBean(OrderService.class);

		try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyTelegramBot(productRepository, orderRepository, orderService, orderProductRepository));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
		  
		  	
		
	}
}
