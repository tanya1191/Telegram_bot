package com.myproject.entity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Customer_Order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderProduct> products; // Products associated with the order

    @Column(name = "chat_id")
    private Long chatId; // Telegram chat ID associated with the user's order

    private boolean confirmed; // Flag to indicate whether the order is confirmed

    @Column(name = "total_price")
    private double totalPrice;


	public List<OrderProduct> getProducts() {
		return products;
	}

	public void setProducts(List<OrderProduct> products) {
		this.products = products;
	}

	public double getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(double totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getChatId() {
		return chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

    // Getters and Setters (omitted for brevity)
    
}
