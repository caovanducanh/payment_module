package com.example.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    private boolean success;
    private String message;
    private String transactionId;
    private TransactionStatus status;
} 