package com.example.pos.model;

import java.time.LocalDate;

public class StaffMember {
    private Long id;
    private String name;
    private String role;
    private String shift;
    private String phone;
    private String email;
    private double monthlySalary;
    private LocalDate joinDate;
    private int allowedLeaves;
    private boolean active;

    public StaffMember() {}

    public StaffMember(Long id, String name, String role, String shift, String phone, String email,
                       double monthlySalary, LocalDate joinDate, int allowedLeaves, boolean active) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.shift = shift;
        this.phone = phone;
        this.email = email;
        this.monthlySalary = monthlySalary;
        this.joinDate = joinDate;
        this.allowedLeaves = allowedLeaves;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public int getAllowedLeaves() { return allowedLeaves; }
    public void setAllowedLeaves(int allowedLeaves) { this.allowedLeaves = allowedLeaves; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
