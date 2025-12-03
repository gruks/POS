package com.example.pos.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates/updates PostgreSQL schema needed by the POS app.
 * Safe to call on every startup (uses CREATE TABLE IF NOT EXISTS).
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            // inventory_items
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory_items (
                    id          BIGSERIAL PRIMARY KEY,
                    name        TEXT NOT NULL UNIQUE,
                    rate        DOUBLE PRECISION NOT NULL,
                    quantity    INTEGER NOT NULL DEFAULT 0,
                    category    TEXT NOT NULL DEFAULT 'Retail',
                    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // menu_categories
            st.execute("""
                CREATE TABLE IF NOT EXISTS menu_categories (
                    id   BIGSERIAL PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE
                )
            """);

            // menu_items
            st.execute("""
                CREATE TABLE IF NOT EXISTS menu_items (
                    id          BIGSERIAL PRIMARY KEY,
                    name        TEXT NOT NULL,
                    price       DOUBLE PRECISION NOT NULL,
                    quantity    INTEGER,
                    description TEXT,
                    image_url   TEXT,
                    category_id BIGINT REFERENCES menu_categories(id)
                )
            """);

            // restaurant_tables
            st.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_tables (
                    id               BIGSERIAL PRIMARY KEY,
                    name             TEXT NOT NULL UNIQUE,
                    capacity         INTEGER NOT NULL,
                    status           TEXT NOT NULL DEFAULT 'Available',
                    reservation_name TEXT,
                    reservation_time TEXT,
                    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // table_sessions
            st.execute("""
                CREATE TABLE IF NOT EXISTS table_sessions (
                    table_id       BIGINT PRIMARY KEY REFERENCES restaurant_tables(id) ON DELETE CASCADE,
                    bill_label     TEXT,
                    customer_name  TEXT,
                    payment_method TEXT,
                    order_type     TEXT,
                    status         TEXT,
                    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // table_session_items
            st.execute("""
                CREATE TABLE IF NOT EXISTS table_session_items (
                    table_id  BIGINT REFERENCES restaurant_tables(id) ON DELETE CASCADE,
                    item_name TEXT NOT NULL,
                    quantity  INTEGER NOT NULL,
                    price     DOUBLE PRECISION NOT NULL
                )
            """);

            // sales
            st.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id            BIGSERIAL PRIMARY KEY,
                    bill_number   BIGINT NOT NULL,
                    customer_name TEXT,
                    payment_method TEXT,
                    order_type    TEXT,
                    subtotal      DOUBLE PRECISION NOT NULL,
                    tax           DOUBLE PRECISION NOT NULL,
                    total         DOUBLE PRECISION NOT NULL,
                    status        TEXT NOT NULL,
                    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    table_id      BIGINT REFERENCES restaurant_tables(id),
                    table_name    TEXT
                )
            """);

            // sale_items
            st.execute("""
                CREATE TABLE IF NOT EXISTS sale_items (
                    id       BIGSERIAL PRIMARY KEY,
                    sale_id  BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
                    name     TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price    DOUBLE PRECISION NOT NULL,
                    total    DOUBLE PRECISION NOT NULL
                )
            """);

            // Optional: default menu categories
            st.execute("""
                INSERT INTO menu_categories (name)
                VALUES ('Beverages'), ('Main Course'), ('Snacks'), ('Desserts'), ('Starters')
                ON CONFLICT (name) DO NOTHING
            """);

        } catch (SQLException e) {
            // Basic logging; you can replace with proper logger
            System.err.println("Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}