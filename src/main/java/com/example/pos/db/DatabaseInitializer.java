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
                    category_id BIGINT REFERENCES menu_categories(id),
                    tax_rate    DOUBLE PRECISION NOT NULL DEFAULT 5.0
                )
            """);
            
            // Add tax_rate column if it doesn't exist (for existing databases)
            st.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                                  WHERE table_name='menu_items' AND column_name='tax_rate') THEN
                        ALTER TABLE menu_items ADD COLUMN tax_rate DOUBLE PRECISION NOT NULL DEFAULT 5.0;
                    END IF;
                END $$;
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

            // staff_members
            st.execute("""
                CREATE TABLE IF NOT EXISTS staff_members (
                    id              BIGSERIAL PRIMARY KEY,
                    name            TEXT NOT NULL,
                    role            TEXT NOT NULL,
                    shift           TEXT NOT NULL,
                    phone           TEXT,
                    email           TEXT,
                    monthly_salary  DOUBLE PRECISION NOT NULL,
                    join_date       DATE NOT NULL,
                    allowed_leaves  INTEGER NOT NULL DEFAULT 2,
                    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // staff_attendance
            st.execute("""
                CREATE TABLE IF NOT EXISTS staff_attendance (
                    id              BIGSERIAL PRIMARY KEY,
                    staff_id        BIGINT NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
                    attendance_date DATE NOT NULL,
                    check_in_time   TIME,
                    check_out_time  TIME,
                    status          TEXT NOT NULL DEFAULT 'Present',
                    notes           TEXT,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    UNIQUE(staff_id, attendance_date)
                )
            """);

            // staff_salary_payments
            st.execute("""
                CREATE TABLE IF NOT EXISTS staff_salary_payments (
                    id              BIGSERIAL PRIMARY KEY,
                    staff_id        BIGINT NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
                    payment_date    DATE NOT NULL,
                    amount_paid     DOUBLE PRECISION NOT NULL,
                    payment_type    TEXT NOT NULL DEFAULT 'Advance',
                    payment_month   TEXT NOT NULL,
                    notes           TEXT,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // staff_leave_records
            st.execute("""
                CREATE TABLE IF NOT EXISTS staff_leave_records (
                    id              BIGSERIAL PRIMARY KEY,
                    staff_id        BIGINT NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
                    leave_date      DATE NOT NULL,
                    leave_type      TEXT NOT NULL DEFAULT 'Sick Leave',
                    reason          TEXT,
                    approved        BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    UNIQUE(staff_id, leave_date)
                )
            """);

            // kitchen_order_tickets (KOT)
            st.execute("""
                CREATE TABLE IF NOT EXISTS kitchen_order_tickets (
                    id              BIGSERIAL PRIMARY KEY,
                    kot_number      BIGINT NOT NULL UNIQUE,
                    table_id        BIGINT REFERENCES restaurant_tables(id),
                    table_name      TEXT,
                    order_type      TEXT NOT NULL DEFAULT 'Dine-In',
                    customer_name   TEXT,
                    status          TEXT NOT NULL DEFAULT 'Pending',
                    priority        TEXT NOT NULL DEFAULT 'Normal',
                    notes           TEXT,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    started_at      TIMESTAMPTZ,
                    completed_at    TIMESTAMPTZ
                )
            """);

            // kot_items
            st.execute("""
                CREATE TABLE IF NOT EXISTS kot_items (
                    id              BIGSERIAL PRIMARY KEY,
                    kot_id          BIGINT NOT NULL REFERENCES kitchen_order_tickets(id) ON DELETE CASCADE,
                    item_name       TEXT NOT NULL,
                    quantity        INTEGER NOT NULL,
                    special_notes   TEXT,
                    status          TEXT NOT NULL DEFAULT 'Pending'
                )
            """);

            // restaurant_info
            st.execute("""
                CREATE TABLE IF NOT EXISTS restaurant_info (
                    id              SERIAL PRIMARY KEY,
                    name            TEXT NOT NULL,
                    address         TEXT NOT NULL,
                    city            TEXT NOT NULL,
                    state           TEXT NOT NULL,
                    pin_code        TEXT NOT NULL,
                    contact_number  TEXT NOT NULL,
                    email           TEXT,
                    website         TEXT,
                    gstin           TEXT,
                    fssai_license   TEXT NOT NULL,
                    logo_path       TEXT,
                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            // Optional: default menu categories
            st.execute("""
                INSERT INTO menu_categories (name)
                VALUES ('Beverages'), ('Main Course'), ('Snacks'), ('Desserts'), ('Starters')
                ON CONFLICT (name) DO NOTHING
            """);

            // Optional: default restaurant tables
            st.execute("""
                INSERT INTO restaurant_tables (name, capacity, status)
                VALUES 
                    ('Table 1', 4, 'Available'),
                    ('Table 2', 4, 'Available'),
                    ('Table 3', 2, 'Available'),
                    ('Table 4', 2, 'Available'),
                    ('Table 5', 6, 'Available'),
                    ('Table 6', 6, 'Available'),
                    ('Table 7', 4, 'Available'),
                    ('Table 8', 4, 'Available'),
                    ('Table 9', 8, 'Available'),
                    ('Table 10', 2, 'Available')
                ON CONFLICT (name) DO NOTHING
            """);

        } catch (SQLException e) {
            // Basic logging; you can replace with proper logger
            System.err.println("Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Database Initializer ===\n");
        System.out.println("Initializing database schema...");
        initialize();
        System.out.println("\n✓ Database initialization complete!");
    }
}