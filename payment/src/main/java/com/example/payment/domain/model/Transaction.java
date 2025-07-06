package com.example.payment.domain.model;

import com.example.payment.domain.service.TransactionBusinessRule;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Transaction {
    private String orderId;
    private double amount;
    private String status;
    private String provider;
    private String transactionId;
    private String redirectUrl;
    private LocalDateTime expiryTime;

    public Transaction(String orderId, double amount, String status, String provider, String transactionId) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.provider = provider;
        this.transactionId = transactionId;
    }

    public Transaction withRedirectUrl(String url) {
        this.redirectUrl = url;
        return this;
    }

    public Transaction markAsPaid(String txnId) {
        TransactionBusinessRule.validatePending(this);
        this.status = "PAID";
        this.transactionId = txnId;
        return this;
    }

    public Transaction markAsFailed() {
        if ("PENDING".equals(this.status)) {
            this.status = "FAILED";
        }
        return this;
    }

    public Transaction markAsExpired() {
        if ("PENDING".equals(this.status) && this.expiryTime != null
                && this.expiryTime.isBefore(LocalDateTime.now())) {
            this.status = "EXPIRED";
        }
        return this;
    }

    public Transaction cancel() {
        if (List.of("PENDING", "FAILED", "EXPIRED").contains(this.status)) {
            this.status = "CANCELLED";
        } else {
            throw new IllegalStateException("Only PENDING, FAILED, or EXPIRED transactions can be cancelled.");
        }
        return this;
    }
}
