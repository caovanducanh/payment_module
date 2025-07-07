package com.example.payment.domain.service;

import com.example.payment.domain.model.Transaction;
import com.example.payment.domain.model.TransactionStatus;

public class TransactionBusinessRule {
    public static void validatePending(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("❌ Transaction must be in PENDING status to proceed. Current status: " + transaction.getStatus());
        }
    }
}
