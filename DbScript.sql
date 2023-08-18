create database store;

use store;

CREATE TABLE product (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10, 2) NOT NULL
);
alter table product
add quantity int;

INSERT INTO product (name, price) VALUES
  ('Product A', 19.99),
  ('Product B', 15.50),
  ('Product C', 8.99),
  ('Product D', 12.75);
  
  select * from product;
  
  
drop table Customer_Order;
CREATE TABLE Customer_Order (
  id INT AUTO_INCREMENT PRIMARY KEY,
  chat_id BIGINT NOT NULL,
  confirmed TINYINT(1) NOT NULL DEFAULT 0,
  total_price DOUBLE NOT NULL
);

drop table order_products;
CREATE TABLE customer_order_products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  product_id INT NOT NULL,
  quantity INT NOT NULL,
  FOREIGN KEY (order_id) REFERENCES CustomerOrder(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);



select * from product;
select * from order;