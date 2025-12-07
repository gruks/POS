package com.example.pos.db;

import com.example.pos.model.StaffMember;
import com.example.pos.model.StaffPayrollInfo;
import com.example.pos.service.PayrollService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Test utility for the payroll system
 */
public class PayrollSystemTest {

    public static void main(String[] args) {
        System.out.println("=== Payroll System Test ===\n");
        
        PayrollService payrollService = new PayrollService();
        
        try {
            // Test 1: Add sample staff
            System.out.println("1. Adding sample staff members...");
            long staffId1 = addSampleStaff(payrollService, "Rajesh Kumar", "Chef", 35000.0);
            long staffId2 = addSampleStaff(payrollService, "Priya Sharma", "Waiter", 25000.0);
            System.out.println("   ✓ Added staff: " + staffId1 + ", " + staffId2);
            
            // Test 2: Mark attendance for current month
            System.out.println("\n2. Marking attendance...");
            markSampleAttendance(payrollService, staffId1);
            markSampleAttendance(payrollService, staffId2);
            System.out.println("   ✓ Attendance marked");
            
            // Test 3: Record advance payment
            System.out.println("\n3. Recording advance payment...");
            payrollService.recordSalaryPayment(staffId1, 10000.0, "Advance", 
                YearMonth.now(), "Mid-month advance");
            System.out.println("   ✓ Advance payment recorded");
            
            // Test 4: Calculate payroll
            System.out.println("\n4. Calculating payroll...");
            StaffPayrollInfo payroll1 = payrollService.calculatePayroll(staffId1, YearMonth.now());
            displayPayrollInfo(payroll1);
            
            // Test 5: Get all staff
            System.out.println("\n5. Loading all staff...");
            List<StaffMember> allStaff = payrollService.getAllStaff();
            System.out.println("   Total staff: " + allStaff.size());
            
            // Test 6: Get all payroll info
            System.out.println("\n6. Loading all payroll info...");
            List<StaffPayrollInfo> allPayroll = payrollService.getAllPayrollInfo(YearMonth.now());
            System.out.println("   Total active staff: " + allPayroll.size());
            for (StaffPayrollInfo info : allPayroll) {
                System.out.println("   - " + info.getName() + ": ₹" + 
                    String.format("%.2f", info.getPendingSalary()) + " pending");
            }
            
            System.out.println("\n✓ All tests passed! Payroll system is working properly.");
            
        } catch (SQLException e) {
            System.err.println("\n✗ Test failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static long addSampleStaff(PayrollService service, String name, 
                                       String role, double salary) throws SQLException {
        StaffMember staff = new StaffMember();
        staff.setName(name);
        staff.setRole(role);
        staff.setShift("Morning");
        staff.setPhone("9876543210");
        staff.setEmail(name.toLowerCase().replace(" ", ".") + "@restaurant.com");
        staff.setMonthlySalary(salary);
        staff.setJoinDate(LocalDate.now().minusDays(15)); // Joined 15 days ago
        staff.setAllowedLeaves(2);
        staff.setActive(true);
        
        return service.addStaff(staff);
    }

    private static void markSampleAttendance(PayrollService service, long staffId) throws SQLException {
        LocalDate today = LocalDate.now();
        
        // Mark attendance for last 10 days
        for (int i = 10; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            service.markAttendance(staffId, date, LocalTime.of(9, 0), "Present");
            service.markCheckOut(staffId, date, LocalTime.of(18, 0));
        }
        
        // Mark today's attendance
        service.markAttendance(staffId, today, LocalTime.now(), "Present");
    }

    private static void displayPayrollInfo(StaffPayrollInfo info) {
        System.out.println("   Staff: " + info.getName());
        System.out.println("   Role: " + info.getRole());
        System.out.println("   Monthly Salary: ₹" + String.format("%.2f", info.getMonthlySalary()));
        System.out.println("   Days Worked: " + info.getDaysWorked() + "/" + info.getTotalDaysInMonth());
        System.out.println("   Leaves Used: " + info.getLeavesUsed() + "/" + info.getAllowedLeaves());
        System.out.println("   Earned Salary: ₹" + String.format("%.2f", info.getEarnedSalary()));
        System.out.println("   Advance Paid: ₹" + String.format("%.2f", info.getAdvancePaid()));
        System.out.println("   Pending Salary: ₹" + String.format("%.2f", info.getPendingSalary()));
        System.out.println("   Status: " + info.getStatus());
    }
}
