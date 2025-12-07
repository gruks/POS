package com.example.pos.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class StaffAttendance {
    private Long id;
    private Long staffId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String status; // Present, Absent, On Leave, Half Day
    private String notes;

    public StaffAttendance() {}

    public StaffAttendance(Long id, Long staffId, LocalDate attendanceDate, LocalTime checkInTime,
                          LocalTime checkOutTime, String status, String notes) {
        this.id = id;
        this.staffId = staffId;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
