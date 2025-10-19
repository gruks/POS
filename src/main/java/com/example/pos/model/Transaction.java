package com.example.pos.model;

import java.time.LocalDateTime;

public class Transaction {

    private String billNumber;
    private LocalDateTime dateTime;
    private String customerName;
    private String type;
    private int amount;
    private String paymentMode;
    private String status;

    // Constructor
    public Transaction(String billNumber, LocalDateTime dateTime, String customerName,
            String type, int amount, String paymentMode, String status) {
        this.billNumber = billNumber;
        this.dateTime = dateTime;
        this.customerName = customerName;
        this.type = type;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.status = status;
    }

    // Getters and Setters
    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Transaction{"
                + "billNumber='" + billNumber + '\''
                + ", dateTime=" + dateTime
                + ", customerName='" + customerName + '\''
                + ", type='" + type + '\''
                + ", amount=" + amount
                + ", paymentMode='" + paymentMode + '\''
                + ", status='" + status + '\''
                + '}';
    }
}
