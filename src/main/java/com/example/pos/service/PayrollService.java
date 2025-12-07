package com.example.pos.service;

import com.example.pos.db.DatabaseConnection;
import com.example.pos.model.StaffAttendance;
import com.example.pos.model.StaffMember;
import com.example.pos.model.StaffPayrollInfo;
import com.example.pos.util.SimpleCache;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing staff payroll, attendance, and salary calculations
 * Optimized with caching and batch operations for better performance
 */
public class PayrollService {
    
    // Cache for staff data (5 minutes TTL)
    private static final SimpleCache<Long, StaffMember> staffCache = new SimpleCache<>(300000);
    
    // Cache for payroll calculations (1 minute TTL)
    private static final SimpleCache<String, StaffPayrollInfo> payrollCache = new SimpleCache<>(60000);
    
    // Cache for all staff list (2 minutes TTL)
    private static final SimpleCache<String, List<StaffMember>> allStaffCache = new SimpleCache<>(120000);

    // ========== STAFF MANAGEMENT ==========

    public long addStaff(StaffMember staff) throws SQLException {
        String sql = """
            INSERT INTO staff_members (name, role, shift, phone, email, monthly_salary, 
                                      join_date, allowed_leaves, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staff.getName());
            ps.setString(2, staff.getRole());
            ps.setString(3, staff.getShift());
            ps.setString(4, staff.getPhone());
            ps.setString(5, staff.getEmail());
            ps.setDouble(6, staff.getMonthlySalary());
            ps.setDate(7, Date.valueOf(staff.getJoinDate()));
            ps.setInt(8, staff.getAllowedLeaves());
            ps.setBoolean(9, staff.isActive());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    clearCache(); // Clear cache after adding staff
                    return id;
                }
            }
        }
        throw new SQLException("Failed to add staff member");
    }

    public List<StaffMember> getAllStaff() throws SQLException {
        // Check cache first
        List<StaffMember> cached = allStaffCache.get("all_staff");
        if (cached != null) {
            return new ArrayList<>(cached); // Return copy to prevent modification
        }
        
        String sql = """
            SELECT id, name, role, shift, phone, email, monthly_salary, 
                   join_date, allowed_leaves, is_active
            FROM staff_members
            ORDER BY name
            """;
        
        List<StaffMember> staffList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                StaffMember member = new StaffMember(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getString("shift"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getDouble("monthly_salary"),
                    rs.getDate("join_date").toLocalDate(),
                    rs.getInt("allowed_leaves"),
                    rs.getBoolean("is_active")
                );
                staffList.add(member);
                // Cache individual staff member
                staffCache.put(member.getId(), member);
            }
        }
        
        // Cache the list
        allStaffCache.put("all_staff", staffList);
        return staffList;
    }
    
    /**
     * Clear all caches (call after data modifications)
     */
    public void clearCache() {
        staffCache.clear();
        payrollCache.clear();
        allStaffCache.clear();
    }
    
    /**
     * Clear cache for specific staff member
     */
    public void clearStaffCache(Long staffId) {
        staffCache.remove(staffId);
        allStaffCache.clear(); // Clear all staff list too
        // Clear payroll cache for this staff
        payrollCache.clear();
    }

    // ========== ATTENDANCE MANAGEMENT ==========

    public void markAttendance(Long staffId, LocalDate date, LocalTime checkInTime, String status) throws SQLException {
        String sql = """
            INSERT INTO staff_attendance (staff_id, attendance_date, check_in_time, status)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (staff_id, attendance_date) 
            DO UPDATE SET check_in_time = EXCLUDED.check_in_time, status = EXCLUDED.status
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, checkInTime != null ? Time.valueOf(checkInTime) : null);
            ps.setString(4, status);
            ps.executeUpdate();
            clearStaffCache(staffId); // Clear cache after marking attendance
        }
    }

    public void markCheckOut(Long staffId, LocalDate date, LocalTime checkOutTime) throws SQLException {
        String sql = """
            UPDATE staff_attendance 
            SET check_out_time = ?
            WHERE staff_id = ? AND attendance_date = ?
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, Time.valueOf(checkOutTime));
            ps.setLong(2, staffId);
            ps.setDate(3, Date.valueOf(date));
            ps.executeUpdate();
            clearStaffCache(staffId); // Clear cache after check-out
        }
    }

    public List<StaffAttendance> getAttendanceForMonth(Long staffId, YearMonth month) throws SQLException {
        String sql = """
            SELECT id, staff_id, attendance_date, check_in_time, check_out_time, status, notes
            FROM staff_attendance
            WHERE staff_id = ? 
              AND EXTRACT(YEAR FROM attendance_date) = ?
              AND EXTRACT(MONTH FROM attendance_date) = ?
            ORDER BY attendance_date
            """;
        
        List<StaffAttendance> attendance = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setInt(2, month.getYear());
            ps.setInt(3, month.getMonthValue());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time checkIn = rs.getTime("check_in_time");
                    Time checkOut = rs.getTime("check_out_time");
                    attendance.add(new StaffAttendance(
                        rs.getLong("id"),
                        rs.getLong("staff_id"),
                        rs.getDate("attendance_date").toLocalDate(),
                        checkIn != null ? checkIn.toLocalTime() : null,
                        checkOut != null ? checkOut.toLocalTime() : null,
                        rs.getString("status"),
                        rs.getString("notes")
                    ));
                }
            }
        }
        return attendance;
    }

    // ========== SALARY CALCULATIONS ==========

    public StaffPayrollInfo calculatePayroll(Long staffId, YearMonth month) throws SQLException {
        // Check cache first
        String cacheKey = staffId + "_" + month.toString();
        StaffPayrollInfo cached = payrollCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        StaffMember staff = getStaffById(staffId);
        if (staff == null) {
            throw new SQLException("Staff member not found");
        }

        StaffPayrollInfo payroll = new StaffPayrollInfo();
        payroll.setStaffId(staffId);
        payroll.setName(staff.getName());
        payroll.setRole(staff.getRole());
        payroll.setShift(staff.getShift());
        payroll.setMonthlySalary(staff.getMonthlySalary());
        payroll.setJoinDate(staff.getJoinDate());
        payroll.setAllowedLeaves(staff.getAllowedLeaves());

        // Calculate days worked
        int daysWorked = countDaysWorked(staffId, month);
        int totalDaysInMonth = month.lengthOfMonth();
        
        // Adjust if joined mid-month
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        if (staff.getJoinDate().isAfter(monthStart)) {
            totalDaysInMonth = (int) ChronoUnit.DAYS.between(staff.getJoinDate(), monthEnd) + 1;
        }

        payroll.setDaysWorked(daysWorked);
        payroll.setTotalDaysInMonth(totalDaysInMonth);

        // Calculate leaves
        int leavesUsed = countLeaves(staffId, month);
        payroll.setLeavesUsed(leavesUsed);

        // Calculate earned salary (daily rate * days worked)
        double dailyRate = staff.getMonthlySalary() / totalDaysInMonth;
        double earnedSalary = dailyRate * daysWorked;
        payroll.setEarnedSalary(earnedSalary);

        // Get advance paid
        double advancePaid = getAdvancePaid(staffId, month);
        payroll.setAdvancePaid(advancePaid);

        // Calculate pending salary
        double pendingSalary = earnedSalary - advancePaid;
        payroll.setPendingSalary(pendingSalary);

        // Get current status
        String status = getTodayStatus(staffId);
        payroll.setStatus(status);

        // Cache the result
        payrollCache.put(cacheKey, payroll);
        
        return payroll;
    }

    private StaffMember getStaffById(Long staffId) throws SQLException {
        // Check cache first
        StaffMember cached = staffCache.get(staffId);
        if (cached != null) {
            return cached;
        }
        
        String sql = """
            SELECT id, name, role, shift, phone, email, monthly_salary, 
                   join_date, allowed_leaves, is_active
            FROM staff_members
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StaffMember member = new StaffMember(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("shift"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getDouble("monthly_salary"),
                        rs.getDate("join_date").toLocalDate(),
                        rs.getInt("allowed_leaves"),
                        rs.getBoolean("is_active")
                    );
                    // Cache it
                    staffCache.put(staffId, member);
                    return member;
                }
            }
        }
        return null;
    }

    private int countDaysWorked(Long staffId, YearMonth month) throws SQLException {
        String sql = """
            SELECT COUNT(*) as days
            FROM staff_attendance
            WHERE staff_id = ? 
              AND EXTRACT(YEAR FROM attendance_date) = ?
              AND EXTRACT(MONTH FROM attendance_date) = ?
              AND status IN ('Present', 'Checked-in')
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setInt(2, month.getYear());
            ps.setInt(3, month.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("days");
                }
            }
        }
        return 0;
    }

    private int countLeaves(Long staffId, YearMonth month) throws SQLException {
        String sql = """
            SELECT COUNT(*) as leaves
            FROM staff_leave_records
            WHERE staff_id = ? 
              AND EXTRACT(YEAR FROM leave_date) = ?
              AND EXTRACT(MONTH FROM leave_date) = ?
              AND approved = true
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setInt(2, month.getYear());
            ps.setInt(3, month.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("leaves");
                }
            }
        }
        return 0;
    }

    private double getAdvancePaid(Long staffId, YearMonth month) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(amount_paid), 0) as total
            FROM staff_salary_payments
            WHERE staff_id = ? AND payment_month = ?
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    private String getTodayStatus(Long staffId) throws SQLException {
        String sql = """
            SELECT status
            FROM staff_attendance
            WHERE staff_id = ? AND attendance_date = CURRENT_DATE
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return "Not Checked-in";
    }

    // ========== SALARY PAYMENT ==========

    public void recordSalaryPayment(Long staffId, double amount, String paymentType, 
                                    YearMonth month, String notes) throws SQLException {
        String sql = """
            INSERT INTO staff_salary_payments (staff_id, payment_date, amount_paid, 
                                              payment_type, payment_month, notes)
            VALUES (?, CURRENT_DATE, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setDouble(2, amount);
            ps.setString(3, paymentType);
            ps.setString(4, month.toString());
            ps.setString(5, notes);
            ps.executeUpdate();
            clearStaffCache(staffId); // Clear cache after payment
        }
    }

    // ========== LEAVE MANAGEMENT ==========

    public void recordLeave(Long staffId, LocalDate leaveDate, String leaveType, 
                           String reason) throws SQLException {
        String sql = """
            INSERT INTO staff_leave_records (staff_id, leave_date, leave_type, reason, approved)
            VALUES (?, ?, ?, ?, true)
            ON CONFLICT (staff_id, leave_date) DO NOTHING
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setDate(2, Date.valueOf(leaveDate));
            ps.setString(3, leaveType);
            ps.setString(4, reason);
            ps.executeUpdate();
            clearStaffCache(staffId); // Clear cache after recording leave
        }
    }

    public List<StaffPayrollInfo> getAllPayrollInfo(YearMonth month) throws SQLException {
        List<StaffMember> allStaff = getAllStaff();
        List<StaffPayrollInfo> payrollList = new ArrayList<>();
        
        for (StaffMember staff : allStaff) {
            if (staff.isActive()) {
                payrollList.add(calculatePayroll(staff.getId(), month));
            }
        }
        
        return payrollList;
    }

    public StaffAttendance getTodayAttendance(Long staffId) throws SQLException {
        String sql = """
            SELECT id, staff_id, attendance_date, check_in_time, check_out_time, status, notes
            FROM staff_attendance
            WHERE staff_id = ? AND attendance_date = CURRENT_DATE
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Time checkIn = rs.getTime("check_in_time");
                    Time checkOut = rs.getTime("check_out_time");
                    return new StaffAttendance(
                        rs.getLong("id"),
                        rs.getLong("staff_id"),
                        rs.getDate("attendance_date").toLocalDate(),
                        checkIn != null ? checkIn.toLocalTime() : null,
                        checkOut != null ? checkOut.toLocalTime() : null,
                        rs.getString("status"),
                        rs.getString("notes")
                    );
                }
            }
        }
        return null;
    }
}
