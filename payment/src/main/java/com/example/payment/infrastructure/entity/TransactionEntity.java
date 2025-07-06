package com.example.payment.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TransactionEntity {
    @Id
    private String orderId;
    private double amount;
    private String status;
    private String provider;
    private String transactionId;

    @Column(length = 2000)
    private String redirectUrl;

    private LocalDateTime expiryTime;
}
