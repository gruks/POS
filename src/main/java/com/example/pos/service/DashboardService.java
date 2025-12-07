package com.example.pos.service;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.util.SimpleCache;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for dashboard analytics and statistics
 * Provides real-time data with caching for performance
 */
public class DashboardService {
    
    // Cache dashboard data for 30 seconds
    private static final SimpleCache<String, Object> dashboardCache = new SimpleCache<>(30000);
    
    /**
     * Get dashboard summary statistics
     */
    public DashboardSummary getDashboardSummary() throws SQLException {
        String cacheKey = "dashboard_summary_" + LocalDate.now();
        DashboardSummary cached = (DashboardSummary) dashboardCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        DashboardSummary summary = new DashboardSummary();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get today's sales
            summary.totalSales = getTodayTotalSales(conn);
            summary.totalOrders = getTodayTotalOrders(conn);
            summary.pendingOrders = getPendingOrders(conn);
            summary.activeTables = getActiveTables(conn);
            summary.lowStockItems = getLowStockItems(conn);
            
            // Calculate growth percentages
            summary.salesGrowth = calculateSalesGrowth(conn);
            summary.ordersGrowth = calculateOrdersGrowth(conn);
            summary.pendingGrowth = calculatePendingGrowth(conn);
        }
        
        dashboardCache.put(cacheKey, summary);
        return summary;
    }
    
    /**
     * Get daily sales trend for the last 7 days
     */
    public List<DailySales> getDailySalesTrend() throws SQLException {
        String cacheKey = "daily_sales_trend";
        @SuppressWarnings("unchecked")
        List<DailySales> cached = (List<DailySales>) dashboardCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        String sql = """
            SELECT DATE(created_at) as sale_date, 
                   SUM(total) as total_sales,
                   COUNT(*) as order_count
            FROM sales
            WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY DATE(created_at)
            ORDER BY sale_date
            """;
        
        List<DailySales> salesList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                salesList.add(new DailySales(
                    rs.getDate("sale_date").toLocalDate(),
                    rs.getDouble("total_sales"),
                    rs.getInt("order_count")
                ));
            }
        }
        
        // Fill missing days with zero sales
        salesList = fillMissingDays(salesList);
        
        dashboardCache.put(cacheKey, salesList);
        return salesList;
    }
    
    /**
     * Get category-wise sales distribution
     */
    public List<CategorySales> getCategorySales() throws SQLException {
        String cacheKey = "category_sales_" + LocalDate.now();
        @SuppressWarnings("unchecked")
        List<CategorySales> cached = (List<CategorySales>) dashboardCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        String sql = """
            SELECT 
                CASE 
                    WHEN si.name IN (SELECT name FROM menu_items WHERE category_id IN 
                        (SELECT id FROM menu_categories WHERE name = 'Beverages')) THEN 'Beverages'
                    WHEN si.name IN (SELECT name FROM menu_items WHERE category_id IN 
                        (SELECT id FROM menu_categories WHERE name = 'Main Course')) THEN 'Food'
                    WHEN si.name IN (SELECT name FROM menu_items WHERE category_id IN 
                        (SELECT id FROM menu_categories WHERE name = 'Desserts')) THEN 'Desserts'
                    ELSE 'Other'
                END as category,
                SUM(si.total) as total_sales,
                SUM(si.quantity) as total_quantity
            FROM sale_items si
            JOIN sales s ON s.id = si.sale_id
            WHERE DATE(s.created_at) = CURRENT_DATE
            GROUP BY category
            ORDER BY total_sales DESC
            """;
        
        List<CategorySales> categoryList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                categoryList.add(new CategorySales(
                    rs.getString("category"),
                    rs.getDouble("total_sales"),
                    rs.getInt("total_quantity")
                ));
            }
        }
        
        dashboardCache.put(cacheKey, categoryList);
        return categoryList;
    }
    
    /**
     * Get recent orders with time ago calculation
     */
    public List<RecentOrder> getRecentOrders(int limit) throws SQLException {
        String sql = """
            SELECT s.bill_number,
                   s.table_name,
                   s.order_type,
                   s.total,
                   s.status,
                   s.created_at
            FROM sales s
            ORDER BY s.created_at DESC
            LIMIT ?
            """;
        
        List<RecentOrder> orders = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime orderTime = timestamp.toLocalDateTime();
                    String timeAgo = calculateTimeAgo(orderTime);
                    
                    String tableInfo = rs.getString("table_name");
                    if (tableInfo == null || tableInfo.isEmpty()) {
                        tableInfo = rs.getString("order_type");
                    }
                    
                    orders.add(new RecentOrder(
                        "#" + rs.getLong("bill_number"),
                        tableInfo,
                        rs.getDouble("total"),
                        rs.getString("status"),
                        timeAgo,
                        orderTime
                    ));
                }
            }
        }
        
        return orders;
    }
    
    // ========== Private Helper Methods ==========
    
    private double getTodayTotalSales(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE DATE(created_at) = CURRENT_DATE";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
    
    private int getTodayTotalOrders(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(created_at) = CURRENT_DATE";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private int getPendingOrders(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales WHERE status IN ('Pending', 'Preparing') AND DATE(created_at) = CURRENT_DATE";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private String getActiveTables(Connection conn) throws SQLException {
        String sql = """
            SELECT COUNT(*) as occupied, 
                   (SELECT COUNT(*) FROM restaurant_tables) as total
            FROM restaurant_tables 
            WHERE status = 'Occupied'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("occupied") + "/" + rs.getInt("total");
            }
            return "0/0";
        }
    }
    
    private int getLowStockItems(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_items WHERE quantity < 10 AND quantity > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private double calculateSalesGrowth(Connection conn) throws SQLException {
        String sql = """
            SELECT 
                COALESCE(SUM(CASE WHEN DATE(created_at) = CURRENT_DATE THEN total ELSE 0 END), 0) as today,
                COALESCE(SUM(CASE WHEN DATE(created_at) = CURRENT_DATE - 1 THEN total ELSE 0 END), 0) as yesterday
            FROM sales
            WHERE DATE(created_at) >= CURRENT_DATE - 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double today = rs.getDouble("today");
                double yesterday = rs.getDouble("yesterday");
                if (yesterday == 0) return today > 0 ? 100.0 : 0.0;
                return ((today - yesterday) / yesterday) * 100;
            }
            return 0.0;
        }
    }
    
    private double calculateOrdersGrowth(Connection conn) throws SQLException {
        String sql = """
            SELECT 
                COUNT(CASE WHEN DATE(created_at) = CURRENT_DATE THEN 1 END) as today,
                COUNT(CASE WHEN DATE(created_at) = CURRENT_DATE - 1 THEN 1 END) as yesterday
            FROM sales
            WHERE DATE(created_at) >= CURRENT_DATE - 1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int today = rs.getInt("today");
                int yesterday = rs.getInt("yesterday");
                if (yesterday == 0) return today > 0 ? 100.0 : 0.0;
                return ((double)(today - yesterday) / yesterday) * 100;
            }
            return 0.0;
        }
    }
    
    private double calculatePendingGrowth(Connection conn) throws SQLException {
        // For pending orders, we compare current hour vs previous hour
        String sql = """
            SELECT 
                COUNT(CASE WHEN created_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour' THEN 1 END) as current_hour,
                COUNT(CASE WHEN created_at >= CURRENT_TIMESTAMP - INTERVAL '2 hours' 
                           AND created_at < CURRENT_TIMESTAMP - INTERVAL '1 hour' THEN 1 END) as previous_hour
            FROM sales
            WHERE status IN ('Pending', 'Preparing')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int current = rs.getInt("current_hour");
                int previous = rs.getInt("previous_hour");
                if (previous == 0) return current > 0 ? 100.0 : 0.0;
                return ((double)(current - previous) / previous) * 100;
            }
            return 0.0;
        }
    }
    
    private List<DailySales> fillMissingDays(List<DailySales> salesList) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        
        Map<LocalDate, DailySales> salesMap = new HashMap<>();
        for (DailySales sales : salesList) {
            salesMap.put(sales.date(), sales);
        }
        
        List<DailySales> filledList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            filledList.add(salesMap.getOrDefault(date, new DailySales(date, 0.0, 0)));
        }
        
        return filledList;
    }
    
    private String calculateTimeAgo(LocalDateTime orderTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(orderTime, now);
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        
        long hours = ChronoUnit.HOURS.between(orderTime, now);
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        
        long days = ChronoUnit.DAYS.between(orderTime, now);
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
    
    /**
     * Clear dashboard cache
     */
    public void clearCache() {
        dashboardCache.clear();
    }
    
    // ========== Data Models ==========
    
    public static class DashboardSummary {
        public double totalSales;
        public int totalOrders;
        public int pendingOrders;
        public String activeTables;
        public int lowStockItems;
        public double salesGrowth;
        public double ordersGrowth;
        public double pendingGrowth;
    }
    
    public record DailySales(LocalDate date, double totalSales, int orderCount) {}
    
    public record CategorySales(String category, double totalSales, int totalQuantity) {}
    
    public record RecentOrder(String billNumber, String tableInfo, double total, 
                             String status, String timeAgo, LocalDateTime orderTime) {}
}
