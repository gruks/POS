package com.example.pos.model;

import java.time.LocalDate;

/**
 * Comprehensive payroll information for a staff member
 */
public class StaffPayrollInfo {
    private Long staffId;
    private String name;
    private String role;
    private String shift;
    private double monthlySalary;
    private LocalDate joinDate;
    private int daysWorked;
    private int totalDaysInMonth;
    private int leavesUsed;
    private int allowedLeaves;
    private double advancePaid;
    private double earnedSalary;
    private double pendingSalary;
    private String status; // Active, On Leave, etc.

    public StaffPayrollInfo() {}

    // Getters and Setters
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public int getDaysWorked() { return daysWorked; }
    public void setDaysWorked(int daysWorked) { this.daysWorked = daysWorked; }

    public int getTotalDaysInMonth() { return totalDaysInMonth; }
    public void setTotalDaysInMonth(int totalDaysInMonth) { this.totalDaysInMonth = totalDaysInMonth; }

    public int getLeavesUsed() { return leavesUsed; }
    public void setLeavesUsed(int leavesUsed) { this.leavesUsed = leavesUsed; }

    public int getAllowedLeaves() { return allowedLeaves; }
    public void setAllowedLeaves(int allowedLeaves) { this.allowedLeaves = allowedLeaves; }

    public double getAdvancePaid() { return advancePaid; }
    public void setAdvancePaid(double advancePaid) { this.advancePaid = advancePaid; }

    public double getEarnedSalary() { return earnedSalary; }
    public void setEarnedSalary(double earnedSalary) { this.earnedSalary = earnedSalary; }

    public double getPendingSalary() { return pendingSalary; }
    public void setPendingSalary(double pendingSalary) { this.pendingSalary = pendingSalary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
