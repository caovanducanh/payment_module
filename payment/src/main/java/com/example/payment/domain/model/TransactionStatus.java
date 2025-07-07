package com.example.payment.domain.model;

public enum TransactionStatus {
    INITIATED,
    PENDING,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    FULLY_REFUNDED,
    PAID,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean canTransitionTo(TransactionStatus newStatus) {
        switch (this) {
            case INITIATED:
                return newStatus == PENDING || newStatus == CANCELLED;
            case PENDING:
                return newStatus == PROCESSING || newStatus == FAILED || newStatus == EXPIRED || newStatus == CANCELLED;
            case PROCESSING:
                return newStatus == AUTHORIZED || newStatus == FAILED || newStatus == CANCELLED;
            case AUTHORIZED:
                return newStatus == CAPTURED || newStatus == FAILED || newStatus == CANCELLED;
            case CAPTURED:
                return newStatus == PAID || newStatus == PARTIALLY_REFUNDED || newStatus == FULLY_REFUNDED;
            case PAID:
                return newStatus == PARTIALLY_REFUNDED || newStatus == FULLY_REFUNDED;
            case PARTIALLY_REFUNDED:
                return newStatus == FULLY_REFUNDED;
            case FULLY_REFUNDED:
                return false;
            case FAILED:
            case EXPIRED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
} 