package com.example.pos.util;

import com.example.pos.model.RestaurantInfo;
import com.example.pos.service.RestaurantInfoService;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Thermal Printer Utility for 3-inch (80mm) receipt printers
 * Supports ESC/POS commands for formatting
 */
public class ThermalPrinter {
    
    // ESC/POS Commands
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    
    // Text alignment
    private static final String ALIGN_LEFT = new String(new char[]{0x1B, 0x61, 0x00});
    private static final String ALIGN_CENTER = new String(new char[]{0x1B, 0x61, 0x01});
    private static final String ALIGN_RIGHT = new String(new char[]{0x1B, 0x61, 0x02});
    
    // Text size
    private static final String SIZE_NORMAL = new String(new char[]{0x1D, 0x21, 0x00});
    private static final String SIZE_DOUBLE = new String(new char[]{0x1D, 0x21, 0x11});
    private static final String SIZE_LARGE = new String(new char[]{0x1D, 0x21, 0x22});
    
    // Text style
    private static final String BOLD_ON = new String(new char[]{0x1B, 0x45, 0x01});
    private static final String BOLD_OFF = new String(new char[]{0x1B, 0x45, 0x00});
    
    // Line feed
    private static final String LINE_FEED = "\n";
    private static final String CUT_PAPER = new String(new char[]{0x1D, 0x56, 0x41, 0x03});
    
    // Width for 3-inch (80mm) printer - approximately 42 characters
    private static final int PAPER_WIDTH = 42;
    
    private final RestaurantInfoService restaurantInfoService = new RestaurantInfoService();
    
    /**
     * Print a bill receipt
     */
    public boolean printBill(BillData billData) {
        try {
            String receipt = generateReceipt(billData);
            return sendToPrinter(receipt);
        } catch (Exception e) {
            System.err.println("Error printing bill: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generate receipt content
     */
    private String generateReceipt(BillData billData) {
        StringBuilder receipt = new StringBuilder();
        
        // Get restaurant info
        RestaurantInfo info = restaurantInfoService.getRestaurantInfo();
        
        // Header - Restaurant Info
        receipt.append(ALIGN_CENTER).append(SIZE_DOUBLE).append(BOLD_ON);
        receipt.append(info != null ? info.getName() : "RESTAURANT POS").append(LINE_FEED);
        receipt.append(BOLD_OFF).append(SIZE_NORMAL);
        
        if (info != null) {
            receipt.append(info.getAddress()).append(LINE_FEED);
            receipt.append(info.getCity()).append(", ").append(info.getState()).append(LINE_FEED);
            receipt.append("PHONE: ").append(info.getContactNumber()).append(LINE_FEED);
            if (info.getGstin() != null && !info.getGstin().isEmpty()) {
                receipt.append("GSTIN: ").append(info.getGstin()).append(LINE_FEED);
            }
            if (info.getFssaiLicense() != null && !info.getFssaiLicense().isEmpty()) {
                receipt.append("FSSAI: ").append(info.getFssaiLicense()).append(LINE_FEED);
            }
        }
        
        receipt.append(LINE_FEED);
        receipt.append(BOLD_ON).append("Retail Invoice").append(BOLD_OFF).append(LINE_FEED);
        receipt.append(printLine()).append(LINE_FEED);
        
        // Date and Time
        receipt.append(ALIGN_LEFT);
        receipt.append("Date: ").append(billData.getDate()).append(LINE_FEED);
        receipt.append(LINE_FEED);
        
        // Customer Name
        if (billData.getCustomerName() != null && !billData.getCustomerName().isEmpty()) {
            receipt.append(billData.getCustomerName()).append(LINE_FEED);
            receipt.append(LINE_FEED);
        }
        
        // Bill Number and Payment Mode
        receipt.append("Bill No: ").append(billData.getBillNumber()).append(LINE_FEED);
        receipt.append("Payment Mode: ").append(billData.getPaymentMode()).append(LINE_FEED);
        
        // Table info if available
        if (billData.getTableName() != null && !billData.getTableName().isEmpty()) {
            receipt.append("Table: ").append(billData.getTableName()).append(LINE_FEED);
        }
        
        // Order Type
        receipt.append("Order Type: ").append(billData.getOrderType()).append(LINE_FEED);
        receipt.append(printLine()).append(LINE_FEED);
        
        // Items Header
        receipt.append(BOLD_ON);
        receipt.append(padRight("Item", 20)).append(padLeft("Qty", 8)).append(padLeft("Amt", 14)).append(LINE_FEED);
        receipt.append(BOLD_OFF);
        receipt.append(printLine()).append(LINE_FEED);
        
        // Items
        for (BillItem item : billData.getItems()) {
            String itemName = truncate(item.getName(), 20);
            receipt.append(padRight(itemName, 20));
            receipt.append(padLeft(String.valueOf(item.getQuantity()), 8));
            receipt.append(padLeft(String.format("%.2f", item.getTotal()), 14));
            receipt.append(LINE_FEED);
        }
        
        receipt.append(printLine()).append(LINE_FEED);
        
        // Totals
        receipt.append(padRight("Sub Total", 28)).append(padLeft(String.format("%.2f", billData.getSubtotal()), 14)).append(LINE_FEED);
        
        // Discount if any
        if (billData.getDiscount() > 0) {
            receipt.append(padRight("(-) Discount", 28)).append(padLeft(String.format("%.2f", billData.getDiscount()), 14)).append(LINE_FEED);
        }
        
        // Tax breakdown
        if (billData.getCgst() > 0) {
            receipt.append(padRight("CGST @ " + billData.getTaxRate()/2 + "%", 28))
                   .append(padLeft(String.format("%.2f", billData.getCgst()), 14)).append(LINE_FEED);
        }
        if (billData.getSgst() > 0) {
            receipt.append(padRight("SGST @ " + billData.getTaxRate()/2 + "%", 28))
                   .append(padLeft(String.format("%.2f", billData.getSgst()), 14)).append(LINE_FEED);
        }
        
        receipt.append(printLine()).append(LINE_FEED);
        
        // Total
        receipt.append(BOLD_ON).append(SIZE_DOUBLE);
        receipt.append(padRight("TOTAL", 21)).append(padLeft("Rs " + String.format("%.2f", billData.getTotal()), 21)).append(LINE_FEED);
        receipt.append(SIZE_NORMAL).append(BOLD_OFF);
        receipt.append(printLine()).append(LINE_FEED);
        
        // Payment details
        receipt.append(padRight(billData.getPaymentMode() + ":", 28))
               .append(padLeft("Rs " + String.format("%.2f", billData.getTotal()), 14)).append(LINE_FEED);
        
        if (billData.getCashTendered() > 0) {
            receipt.append(padRight("Cash tendered:", 28))
                   .append(padLeft("Rs " + String.format("%.2f", billData.getCashTendered()), 14)).append(LINE_FEED);
            double change = billData.getCashTendered() - billData.getTotal();
            if (change > 0) {
                receipt.append(padRight("Change:", 28))
                       .append(padLeft("Rs " + String.format("%.2f", change), 14)).append(LINE_FEED);
            }
        }
        
        receipt.append(LINE_FEED);
        
        // Footer
        receipt.append(ALIGN_CENTER);
        receipt.append("E & O.E").append(LINE_FEED);
        receipt.append(LINE_FEED);
        receipt.append("Thank you! Visit again!").append(LINE_FEED);
        receipt.append(LINE_FEED);
        receipt.append(LINE_FEED);
        receipt.append(LINE_FEED);
        
        // Cut paper
        receipt.append(CUT_PAPER);
        
        return receipt.toString();
    }
    
    /**
     * Send receipt to printer
     */
    private boolean sendToPrinter(String receipt) {
        try {
            // Find thermal printer
            PrintService printer = findThermalPrinter();
            
            if (printer == null) {
                System.err.println("No thermal printer found. Using default printer.");
                printer = PrintServiceLookup.lookupDefaultPrintService();
            }
            
            if (printer == null) {
                System.err.println("No printer available!");
                return false;
            }
            
            System.out.println("Printing to: " + printer.getName());
            
            // Create print job
            DocPrintJob job = printer.createPrintJob();
            
            // Convert string to bytes
            byte[] bytes = receipt.getBytes(StandardCharsets.UTF_8);
            InputStream is = new ByteArrayInputStream(bytes);
            
            // Create document
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(is, flavor, null);
            
            // Print attributes
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            
            // Print
            job.print(doc, attrs);
            
            System.out.println("Print job sent successfully!");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending to printer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Find thermal printer (looks for common thermal printer names)
     */
    private PrintService findThermalPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        
        // Common thermal printer keywords
        String[] keywords = {"thermal", "pos", "receipt", "80mm", "58mm", "xprinter", "epson", "star"};
        
        for (PrintService service : services) {
            String name = service.getName().toLowerCase();
            for (String keyword : keywords) {
                if (name.contains(keyword)) {
                    return service;
                }
            }
        }
        
        return null;
    }
    
    /**
     * List all available printers
     */
    public static void listAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        System.out.println("Available Printers:");
        for (int i = 0; i < services.length; i++) {
            System.out.println((i + 1) + ". " + services[i].getName());
        }
    }
    
    // Helper methods
    private String printLine() {
        return "-".repeat(PAPER_WIDTH);
    }
    
    private String padRight(String text, int length) {
        if (text.length() >= length) return text.substring(0, length);
        return text + " ".repeat(length - text.length());
    }
    
    private String padLeft(String text, int length) {
        if (text.length() >= length) return text.substring(0, length);
        return " ".repeat(length - text.length()) + text;
    }
    
    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length - 3) + "...";
    }
    
    // Data classes
    public static class BillData {
        private String billNumber;
        private String date;
        private String customerName;
        private String paymentMode;
        private String tableName;
        private String orderType;
        private List<BillItem> items;
        private double subtotal;
        private double discount;
        private double cgst;
        private double sgst;
        private double taxRate;
        private double total;
        private double cashTendered;
        
        // Getters and setters
        public String getBillNumber() { return billNumber; }
        public void setBillNumber(String billNumber) { this.billNumber = billNumber; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getPaymentMode() { return paymentMode; }
        public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
        
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        
        public List<BillItem> getItems() { return items; }
        public void setItems(List<BillItem> items) { this.items = items; }
        
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
        
        public double getDiscount() { return discount; }
        public void setDiscount(double discount) { this.discount = discount; }
        
        public double getCgst() { return cgst; }
        public void setCgst(double cgst) { this.cgst = cgst; }
        
        public double getSgst() { return sgst; }
        public void setSgst(double sgst) { this.sgst = sgst; }
        
        public double getTaxRate() { return taxRate; }
        public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
        
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
        
        public double getCashTendered() { return cashTendered; }
        public void setCashTendered(double cashTendered) { this.cashTendered = cashTendered; }
    }
    
    public static class BillItem {
        private String name;
        private int quantity;
        private double price;
        private double total;
        
        public BillItem(String name, int quantity, double price, double total) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.total = total;
        }
        
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getTotal() { return total; }
    }
}
