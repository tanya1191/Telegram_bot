package com.myproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.myproject.entity.Order;
import com.myproject.entity.OrderProduct;
import com.myproject.entity.Product;
import com.myproject.repository.OrderProductRepository;
import com.myproject.repository.OrderRepository;
import com.myproject.repository.ProductRepository;
import com.myproject.service.OrderService;

@SuppressWarnings("deprecation")
@Component
public class MyTelegramBot extends TelegramLongPollingBot {

	private final ProductRepository productRepository;
	
    private final OrderRepository orderRepository;
    
    private final OrderProductRepository orderProductRepository;

    private final OrderService orderService;
	public MyTelegramBot(ProductRepository productRepository, OrderRepository orderRepository,
			OrderService orderService, OrderProductRepository orderProductRepository) {
		super();
		this.productRepository = productRepository;
		this.orderRepository = orderRepository;
		this.orderProductRepository = orderProductRepository;
		this.orderService = orderService;
	}

    
	@Value("Rs.{bot.username}") 
	private String botUsername;
	
	@Value("Rs.{bot.token}") 
	private String botToken;

    private final Map<Long, List<Product>> userCarts = new HashMap<Long, List<Product>>();

	/*
	 * public MyTelegramBot(ProductRepository productRepository, OrderRepository
	 * orderRepository) { this.productRepository = productRepository;
	 * this.orderRepository = orderRepository; }
	 */

    
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText().trim().toLowerCase();

            switch (messageText) {
                case "/start":
                    sendProductList(chatId);
                    break;
                case "/order":
                	handleViewOrderCommand(chatId);
                    break;
                case "/delete":
                	sendCartItemsForDeletion(chatId);
                	break;
                default:
                    // Handle other commands or messages
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();
        String callbackQueryId = callbackQuery.getId();

        // Check if the callback is for a product selection
        if (callbackData.startsWith("product:")) {
            // Extract the product ID from the callback data
            String[] dataParts = callbackData.split(":");
            if (dataParts.length >= 2) {
                long productId = Long.parseLong(dataParts[1]);

                // Here, you can fetch the selected product details from the database
                Product product = productRepository.findById(productId).orElse(null);

                if (product != null) {
                
                    // Ask the user to enter the quantity for the selected product
                    //sendTextMessage(chatId, "Enter the quantity for " + product.getName() + ":");
                    sendQuantitySelection(chatId, product);
                    // You can store the product and the user's state (awaitingQuantity) in a map or database
                    // so that you can keep track of the user's current action
                }
            }
        } else if (callbackData.startsWith("delete:")) {
            // Extract the product ID from the callback data
            String[] dataParts = callbackData.split(":");
            if (dataParts.length >= 2) {
                long productId = Long.parseLong(dataParts[1]);
                List<Product> userCart = userCarts.get(chatId);
                // Here, you can fetch the selected product details from the database
                Product product = productRepository.findById(productId).orElse(null);

                
                
                
                if (product != null) {
                    // Check if the user has this product in their order
                    
                    if (userCart != null) {
                        // Remove the product from the user's order
                    	Iterator<Product> itr = userCart.iterator();
                        while (itr.hasNext()) {
                            if (itr.next().getId() == productId) {
                                itr.remove();
                            }
                        }
                        sendTextMessage(chatId, "Removed " + product.getName() + " from your order.");
                        // Update the message to reflect the updated order
                        sendOrderList(chatId);
                        //updateOrderMessage(chatId, messageId, userCart);
                    } else {
                        sendTextMessage(chatId, "You don't have " + product.getName() + " in your order.");
                    }
                }
            }
        }  else if (callbackData.startsWith("confirm_order")) {
            // Here, we assume that the callbackData has the format "confirm:"
            // indicating that the user has confirmed their order.

            // Retrieve the user's order from the userOrders map
            List<Product> userCart = userCarts.get(chatId);

            if (userCart != null && !userCart.isEmpty()) {
            	
            	
                // Save the order to the database with status confirmed
                //Order order = orderService.findOrCreateOrderForChat(chatId);
                //order.setConfirmed(true);
                //orderRepository.save(order);
            	
            	  // Calculate the total price of the order
                double totalPrice = calculateTotalPrice(userCart);

                // Create the Order object
                Order order = new Order();
                order.setChatId(chatId);
                order.setTotalPrice(totalPrice);

                // Save the order to the database using the OrderRepository
                Order savedOrder= orderRepository.save(order);

                List<OrderProduct> orderProducts=new ArrayList<OrderProduct>();
                
                for (Product product : userCart) {

                    OrderProduct orderProduct = new OrderProduct();
                    orderProduct.setOrder(savedOrder);
                    orderProduct.setProductId(product.getId());
                    orderProduct.setQuantity(product.getQuantity());
                    orderProducts.add(orderProduct);	
				}
                
                orderProductRepository.saveAll(orderProducts);
                
                
                // Clear the user's order after it has been confirmed
                userCart.clear();

                // Update the message to reflect the confirmed order
               // updateOrderMessage(chatId, messageId, userCart);

                // Send a confirmation message to the user
                sendTextMessage(chatId, "Your order has been confirmed. Thank you!");

                // You can perform additional actions here, such as sending a notification to the store, etc.
            }
            else {
                // If the user's order is empty, inform them that they need to add products to their order first.
                sendTextMessage(chatId, "Your order is empty. Please add products before confirming.");
            }
        }
	else if (callbackData.startsWith("cancel_order")) {
       	 userCarts.remove(chatId);

       	sendTextMessage(chatId, "Your order has been cancelled. Thank you!");
       }

        else if (callbackData.startsWith("quantity:")) {
        	handleQuantitySelection(callbackQuery);
        }
    }

    private void sendProductList(long chatId) {
    	//orderService.findOrCreateOrderForChat(chatId);
        List<Product> products = productRepository.findAll();

        if (null==products || products.isEmpty()) {
            sendTextMessage(chatId, "No products available.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a product:");
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product product : products) {
            String buttonText = product.getName() + " (Rs." + product.getPrice() + ")";
            String callbackData = "product:" + product.getId() + ":";
            InlineKeyboardButton button = new InlineKeyboardButton(buttonText);
            button.setCallbackData(callbackData);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        		message.setChatId(chatId);
        		message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void updateOrderMessage(long chatId, int messageId, List<Product> userOrder) {
        // Create a StringBuilder to construct the new message text
        StringBuilder newMessageText = new StringBuilder("Your Order:\n");

        
        // Append the list of products in the user's order to the message text
        int itemNumber = 1;
        for (Product product : userOrder) {
            newMessageText.append(itemNumber).append(". ").append(product.getName()).append(" (Rs.").append(product.getPrice()).append(")\n");
            itemNumber++;
        }

        // Add a message for an empty order
        if (userOrder.isEmpty()) {
            newMessageText.append("Your order is empty.\n");
        }

        // Create a new inline keyboard markup for the updated order message
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> confirmRow = new ArrayList<>();

        // Add a Confirm Order button if the user's order is not empty
        if (!userOrder.isEmpty()) {
            InlineKeyboardButton confirmButton = new InlineKeyboardButton("Confirm Order");
            		confirmButton.setCallbackData("confirm:");
            confirmRow.add(confirmButton);
            rows.add(confirmRow);
        }

        keyboardMarkup.setKeyboard(rows);

        // Create the EditMessageText object with the updated message text and inline keyboard
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(newMessageText.toString());
        editMessageText.setReplyMarkup(keyboardMarkup);

        try {
            // Send the updated message to Telegram
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    
    private void handleViewOrderCommand(long chatId) {
        sendOrderList(chatId);
    }
    
    private void sendOrderList(long chatId) {
        
    	 List<Product> userOrder = userCarts.get(chatId);
    	
    	if (null==userOrder || userOrder.isEmpty()) {
            sendTextMessage(chatId, "Your order is empty.");
            return;
        }

        StringBuilder messageText = new StringBuilder();

		/*
		 * for (int i = 0; i < userOrder.size(); i++) { Product product =
		 * userOrder.get(i); int quantity = product.getQuantity();
		 * 
		 * // Calculate the subtotal for each product (price * quantity) double subtotal
		 * = product.getPrice() * quantity;
		 * 
		 * // Add the product details to the message text messageText.append(i +
		 * 1).append(". ").append(product.getName()) .append(" (Qty: ").append(quantity)
		 * .append(", Price: Rs.").append(product.getPrice())
		 * .append(", Subtotal: Rs.").append(subtotal) .append(")\n");
		 * 
		 * // Add the subtotal to the total bill totalBill += subtotal; }
		 */        
        String tableText = formatTable(userOrder);
        messageText.append(tableText);

        // Create the message with the formatted table
        SendMessage message = new SendMessage();
        		message.setChatId(chatId);
        		message.setText(tableText);
        		message.setParseMode(ParseMode.HTML);

        // Send the message
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        
        sendConfirmOrderKeyboard(chatId);    
        
    }
    
    private int getProductQuantityInOrder(List<Product> userOrder, Product product) {
        int quantity = 0;
        for (Product p : userOrder) {
            if (p.getId() == product.getId()) {
                quantity++;
            }
        }
        return quantity;
    }
    
    
    private void addToUserCart(long chatId, long orderId, Product product, int quantity) {
        // Retrieve the user's cart from the map (create a new cart if not present)
        List<Product> userCart = userCarts.getOrDefault(orderId, new ArrayList<>());

        // Update the product's quantity in the cart or add it as a new product
        boolean productExistsInCart = false;
        for (Product cartProduct : userCart) {
            if (cartProduct.getId() == product.getId()) {
                cartProduct.setQuantity(quantity);
                productExistsInCart = true;
                break;
            }
        }

        if (!productExistsInCart) {
            // Add the product to the cart if it doesn't exist in the cart already
            product.setQuantity(quantity);
            userCart.add(product);
        }

        // Update the user's cart in the map
        userCarts.put(orderId, userCart);
    }
    
    private void addToUserCart(long chatId, Product product, int quantity) {
        // Retrieve the user's cart from the map (create a new cart if not present)
        List<Product> userCart = userCarts.getOrDefault(chatId, new ArrayList<>());

        // Check if the product already exists in the user's cart
        boolean productExistsInCart = false;
        for (Product cartProduct : userCart) {
            if (cartProduct.getId() == product.getId()) {
                // If the product exists, update its quantity
                cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
                productExistsInCart = true;
                break;
            }
        }

        if (!productExistsInCart) {
            // If the product does not exist in the cart, add it with the selected quantity
            Product cartProduct = new Product();
            cartProduct.setId(product.getId());
            cartProduct.setName(product.getName());
            cartProduct.setPrice(product.getPrice());
            cartProduct.setQuantity(quantity);

            userCart.add(cartProduct);
        }

        // Update the user's cart in the map
        userCarts.put(chatId, userCart);
    }

    private void sendConfirmationMessage(long chatId, Product product, int quantity) {
        // Send a message to confirm the product and quantity added to the cart
        String message = "Added to cart:\n"
                + "Product: " + product.getName() + "\n"
                + "Price: " + product.getPrice() + "\n"
                + "Quantity: " + quantity;
        sendTextMessage(chatId, message);
    }
    
    private void sendQuantitySelection(long chatId, Product product) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Please select the quantity for " + product.getName() + ":");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Add buttons for different quantity options (e.g., 1, 2, 3, etc.)
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(i));
            button.setCallbackData("quantity:" + product.getId() + ":" + i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);

        // Send the message with the inline keyboard for quantity selection
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    
    private void handleQuantitySelection(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        // Parse the quantity from the callback data
        String[] dataParts = callbackData.split(":");
        if (dataParts.length == 3) {
            long productId = Long.parseLong(dataParts[1]);
            int quantity = Integer.parseInt(dataParts[2]);

            // Assuming you have the ProductRepository to fetch the product details
            Product product = productRepository.findById(productId).orElse(null);

            if (product != null) {
                // Process the user's quantity selection (e.g., add to cart, etc.)
                // Here, you can use the product and quantity as needed
                // For example, you can add the product to the user's cart with the selected quantity
               addToUserCart(chatId, product, quantity);
            	//	addToUserCart(chatId, productId, product, quantity);
                // Send a confirmation message to the user
                String confirmationMessage = "Added " + quantity + " " + product.getName() + " to your cart.";
                sendTextMessage(chatId, confirmationMessage);
                sendProductList(chatId);
            } else {
                // Handle the case when the product is not found
            	sendTextMessage(chatId, "Product not found.");
            }
        }
    }
    
    // Method to send the confirm order message with inline keyboard
    private void sendConfirmOrderKeyboard(long chatId) {
        // Create an inline keyboard with two buttons: Confirm and Cancel
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Button for confirming the order
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        		confirmButton.setText("Confirm Order");
        		confirmButton.setCallbackData("confirm_order");

        // Button for canceling the order
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Cancel Order");
        cancelButton.setCallbackData("cancel_order");

        // Add buttons to the keyboard row
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(confirmButton);
        row.add(cancelButton);
        rows.add(row);

        // Set the keyboard rows to the inline keyboard
        inlineKeyboard.setKeyboard(rows);

        // Create a message with the inline keyboard
        SendMessage message = new SendMessage();
        		message.setChatId(chatId);
        		message.setText("Please confirm your order:");
        		message.setReplyMarkup(inlineKeyboard);

        // Send the message with the inline keyboard
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCartItemsForDeletion(long chatId) {
    	
    	// Retrieve the user's cart from the map (create a new cart if not present)
        List<Product> cartItems = userCarts.getOrDefault(chatId, new ArrayList<>());

        SendMessage message = new SendMessage();
        		message.setChatId(chatId);
        		message.setText("Select an item to delete:");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product cartItem : cartItems) {
            // Button for deleting the item
            InlineKeyboardButton deleteButton = new InlineKeyboardButton();
            		deleteButton.setText(cartItem.getName());
            deleteButton.setCallbackData("delete:" + cartItem.getId());

            // Add the button to a new row
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(deleteButton);
            rows.add(row);
        }

        // Set the keyboard rows to the inline keyboard
        inlineKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboard);

        // Send the message with the inline keyboard
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private double calculateTotalPrice(List<Product> cartItems) {
        double totalPrice = 0;
        for (Product product : cartItems) {
            totalPrice += (product.getPrice() * product.getQuantity());
        }
        return totalPrice;
    }
    
    private String formatTable(List<Product> products) {
        StringBuilder table = new StringBuilder();

        // Header row
        table.append("<pre>Product Name   | Price (₹)  | Quantity | Total (₹)\n");
        table.append("----------------------------------------------\n");

        // Data rows
        double totalPrice = 0;
        for (Product product : products) {
            double totalForProduct = product.getPrice() * product.getQuantity();
            totalPrice += totalForProduct;
            table.append(String.format("%-15s | ₹%-5.2f | %-8d | ₹%-5.2f\n", product.getName(), product.getPrice(), product.getQuantity(), totalForProduct));
        }

        // Total price row
        table.append("\nTotal Price: ₹").append(String.format("%.2f", totalPrice));
        table.append("</pre>");
        return table.toString();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    // Implement other methods for handling order placement, product deletion, and order confirmation

    @Override
    public String getBotUsername() {
        return "tanya_grocery_store_bot";
    }

    @Override
    public String getBotToken() {
        return "6627411043:AAFFXUc0ofBujstkEnWsI1sW_98Y4pY7ESY";
    }
}

