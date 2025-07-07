package com.example.payment.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import com.example.payment.domain.model.TransactionStatus;
import com.example.payment.domain.model.PaymentGatewayType;

@Entity
@Getter
@Setter
public class TransactionEntity {
    @Id
    private String orderId;
    private double amount;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    @Enumerated(EnumType.STRING)
    private PaymentGatewayType provider;
    private String transactionId;

    @Column(length = 2000)
    private String redirectUrl;

    private LocalDateTime expiryTime;
}
