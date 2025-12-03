package com.example.pos.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.MenuItem;
import com.example.pos.model.MenuProduct;

/**
 * Provides JDBC access to menu items and categories.
 */
public class MenuService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Beverages", "Main Course", "Snacks", "Desserts", "Starters");

    public Map<Long, String> loadCategories() {
        String sql = "SELECT id, name FROM menu_categories ORDER BY name";
        Map<Long, String> categories = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.put(rs.getLong("id"), rs.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load categories", ex);
        }
        return categories;
    }

    public List<MenuProduct> loadProducts() {
        String sql = """
                SELECT i.id,
                       i.name,
                       i.price,
                       i.quantity,
                       i.description,
                       i.image_url,
                       i.category_id,
                       c.name AS category_name
                FROM menu_items i
                LEFT JOIN menu_categories c ON c.id = i.category_id
                ORDER BY i.name
                """;
        List<MenuProduct> products = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load menu items", ex);
        }
        return products;
    }

    public List<MenuItem> loadMenuItemsForBilling() {
        String sql = """
                SELECT i.name, i.price, c.name AS category_name, i.image_url, i.quantity
                FROM menu_items i
                LEFT JOIN menu_categories c ON c.id = i.category_id
                ORDER BY i.name
                """;
        List<MenuItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer quantity = rs.getObject("quantity") == null ? null : rs.getInt("quantity");
                items.add(new MenuItem(
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category_name"),
                        rs.getString("image_url"),
                        quantity));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load billing menu", ex);
        }
        return items;
    }

    public long ensureCategory(String name) {
        Objects.requireNonNull(name, "Category name required");
        String normalized = name.trim();
        String find = "SELECT id FROM menu_categories WHERE LOWER(name) = LOWER(?)";
        String insert = "INSERT INTO menu_categories (name) VALUES (?)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(find)) {
                ps.setString(1, normalized);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, normalized);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
            throw new IllegalStateException("Failed to create category '" + name + "'");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to ensure category", ex);
        }
    }

    public MenuProduct createProduct(MenuProduct product) {
        Objects.requireNonNull(product, "Product required");
        String sql = """
                INSERT INTO menu_items (name, price, quantity, description, image_url, category_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setDouble(2, product.getPrice());
            ps.setInt(3, product.getQuantity());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getImageUrl());
            ps.setLong(6, product.getCategoryId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findProductById(connection, keys.getLong(1));
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create menu item", ex);
        }
    }

    public void updateProduct(MenuProduct product) {
        Objects.requireNonNull(product, "Product required");
        String sql = """
                UPDATE menu_items
                SET name = ?, price = ?, quantity = ?, description = ?, image_url = ?, category_id = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setDouble(2, product.getPrice());
            ps.setInt(3, product.getQuantity());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getImageUrl());
            ps.setLong(6, product.getCategoryId());
            ps.setLong(7, product.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update menu item", ex);
        }
    }

    public void deleteProduct(long id) {
        String sql = "DELETE FROM menu_items WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete menu item", ex);
        }
    }

    public void renameCategory(long categoryId, String newName) {
        String sql = "UPDATE menu_categories SET name = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setLong(2, categoryId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to rename category", ex);
        }
    }

    public void deleteCategory(long categoryId) {
        String reassign = "UPDATE menu_items SET category_id = NULL WHERE category_id = ?";
        String delete = "DELETE FROM menu_categories WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement psReassign = connection.prepareStatement(reassign);
                 PreparedStatement psDelete = connection.prepareStatement(delete)) {
                psReassign.setLong(1, categoryId);
                psReassign.executeUpdate();
                psDelete.setLong(1, categoryId);
                psDelete.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete category", ex);
        }
    }

    public long countProductsInCategory(long categoryId) {
        String sql = "SELECT COUNT(*) AS total FROM menu_items WHERE category_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count items in category", ex);
        }
    }

    public void ensureDefaultCategoriesExist() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            for (String category : DEFAULT_CATEGORIES) {
                ensureCategoryWithConnection(connection, category);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to ensure default categories", ex);
        }
    }

    private void ensureCategoryWithConnection(Connection connection, String name) throws SQLException {
        String check = "SELECT id FROM menu_categories WHERE LOWER(name) = LOWER(?)";
        String insert = "INSERT INTO menu_categories (name) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(check)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    private MenuProduct findProductById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT i.id,
                       i.name,
                       i.price,
                       i.quantity,
                       i.description,
                       i.image_url,
                       i.category_id,
                       c.name AS category_name
                FROM menu_items i
                LEFT JOIN menu_categories c ON c.id = i.category_id
                WHERE i.id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        }
        return null;
    }

    private MenuProduct mapProduct(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long categoryId = rs.getObject("category_id") == null ? 0 : rs.getLong("category_id");
        return new MenuProduct(
                id,
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getString("category_name"),
                categoryId,
                rs.getInt("quantity"),
                rs.getString("description"),
                rs.getString("image_url"));
    }
}

