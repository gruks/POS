package com.example.pos.service;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.RestaurantInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RestaurantInfoService {

    public RestaurantInfo getRestaurantInfo() {
        String sql = "SELECT * FROM restaurant_info LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                RestaurantInfo info = new RestaurantInfo();
                info.setName(rs.getString("name"));
                info.setAddress(rs.getString("address"));
                info.setCity(rs.getString("city"));
                info.setState(rs.getString("state"));
                info.setPinCode(rs.getString("pin_code"));
                info.setContactNumber(rs.getString("contact_number"));
                info.setEmail(rs.getString("email"));
                info.setWebsite(rs.getString("website"));
                info.setGstin(rs.getString("gstin"));
                info.setFssaiLicense(rs.getString("fssai_license"));
                info.setLogoPath(rs.getString("logo_path"));
                return info;
            }
        } catch (SQLException e) {
            System.err.println("Error loading restaurant info: " + e.getMessage());
        }
        return null;
    }

    public void saveRestaurantInfo(RestaurantInfo info) {
        String sql = """
            INSERT INTO restaurant_info (name, address, city, state, pin_code, contact_number, email, website, gstin, fssai_license, logo_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                address = EXCLUDED.address,
                city = EXCLUDED.city,
                state = EXCLUDED.state,
                pin_code = EXCLUDED.pin_code,
                contact_number = EXCLUDED.contact_number,
                email = EXCLUDED.email,
                website = EXCLUDED.website,
                gstin = EXCLUDED.gstin,
                fssai_license = EXCLUDED.fssai_license,
                logo_path = EXCLUDED.logo_path,
                updated_at = NOW()
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, info.getName());
            ps.setString(2, info.getAddress());
            ps.setString(3, info.getCity());
            ps.setString(4, info.getState());
            ps.setString(5, info.getPinCode());
            ps.setString(6, info.getContactNumber());
            ps.setString(7, info.getEmail());
            ps.setString(8, info.getWebsite());
            ps.setString(9, info.getGstin());
            ps.setString(10, info.getFssaiLicense());
            ps.setString(11, info.getLogoPath());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save restaurant info", e);
        }
    }
}
