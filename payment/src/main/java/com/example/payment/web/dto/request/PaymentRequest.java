package com.example.payment.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class PaymentRequest {
    @NotBlank(message = "Provider is required")
    private String provider;
    @NotBlank(message = "OrderId is required")
    private String orderId;
    @Positive(message = "Amount must be positive")
    private double amount;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}