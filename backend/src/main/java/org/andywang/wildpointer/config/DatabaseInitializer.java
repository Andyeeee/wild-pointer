package org.andywang.wildpointer.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the SQLite database schema on application startup.
 * This ensures the users table exists before any API requests arrive.
 */
@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create users table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "nickname VARCHAR(50), " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "is_active BOOLEAN DEFAULT 1, " +
                    "avatar VARCHAR(500), " +
                    "bio VARCHAR(500), " +
                    "default_distance INTEGER, " +
                    "default_duration INTEGER" +
                    ")";

            statement.execute(createTableSQL);
            System.out.println("✓ Database initialized successfully. Users table is ready.");

        } catch (Exception e) {
            System.err.println("✗ Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
