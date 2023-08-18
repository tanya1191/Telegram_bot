package com.myproject.dbconnect;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

//@Component
public class DatabaseConnectionChecker {

    private final DataSource dataSource;

    @Autowired
    public DatabaseConnectionChecker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database.");
            e.printStackTrace();
        }
    }
}
