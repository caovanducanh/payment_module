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
    private TransactionStatus status;
    private PaymentGatewayType provider;
    private String transactionId;
    private String redirectUrl;
    private LocalDateTime expiryTime;

    public Transaction(String orderId, double amount, TransactionStatus status, PaymentGatewayType provider, String transactionId) {
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
        this.status = TransactionStatus.PAID;
        this.transactionId = txnId;
        return this;
    }

    public Transaction markAsFailed() {
        if (this.status == TransactionStatus.PENDING) {
            this.status = TransactionStatus.FAILED;
        }
        return this;
    }

    public Transaction markAsExpired() {
        if (this.status == TransactionStatus.PENDING && this.expiryTime != null
                && this.expiryTime.isBefore(LocalDateTime.now())) {
            this.status = TransactionStatus.EXPIRED;
        }
        return this;
    }

    public Transaction cancel() {
        if (List.of(TransactionStatus.PENDING, TransactionStatus.FAILED, TransactionStatus.EXPIRED).contains(this.status)) {
            this.status = TransactionStatus.CANCELLED;
        } else {
            throw new IllegalStateException("Only PENDING, FAILED, or EXPIRED transactions can be cancelled.");
        }
        return this;
    }
}
