package com.parking.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections and table creation scripts.
 */
public class DatabaseManager {
    private static String dbUrl = "jdbc:sqlite:parking_lot.db";

    public static void setDbUrl(String newDbUrl) {
        dbUrl = newDbUrl;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public static void initializeDatabase() {
        String createSlotsTable = """
            CREATE TABLE IF NOT EXISTS slots (
                slot_id TEXT PRIMARY KEY,
                slot_type TEXT NOT NULL,
                occupied INTEGER NOT NULL DEFAULT 0,
                vehicle_license TEXT,
                vehicle_type TEXT
            );
        """;

        String createTicketsTable = """
            CREATE TABLE IF NOT EXISTS tickets (
                ticket_id TEXT PRIMARY KEY,
                license_plate TEXT NOT NULL,
                vehicle_type TEXT NOT NULL,
                slot_id TEXT NOT NULL,
                entry_time TEXT NOT NULL,
                exit_time TEXT,
                fee REAL NOT NULL DEFAULT 0.0,
                paid INTEGER NOT NULL DEFAULT 0
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSlotsTable);
            stmt.execute(createTicketsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database: " + e.getMessage(), e);
        }
    }
}
