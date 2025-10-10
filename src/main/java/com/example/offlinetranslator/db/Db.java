package com.example.offlinetranslator.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    private final String url;
    public Db(String url) { this.url = url; }

    public void init() {
        try (var conn = get()) {
            try (var st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS history (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      source_lang TEXT,
                      target_lang TEXT,
                      input_text  TEXT,
                      output_text TEXT
                    )
                """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    public Connection get() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
