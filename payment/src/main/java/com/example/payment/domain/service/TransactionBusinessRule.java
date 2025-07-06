package com.example.payment.domain.service;

import com.example.payment.domain.model.Transaction;

public class TransactionBusinessRule {
    public static void validatePending(Transaction transaction) {
        if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
            throw new IllegalStateException("❌ Transaction must be in PENDING status to proceed. Current status: " + transaction.getStatus());
        }
    }
}
