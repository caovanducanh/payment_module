package com.example.payment.web.dto.request;

public class PaymentRequest {
    private String provider;
    private String orderId;
    private double amount;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}