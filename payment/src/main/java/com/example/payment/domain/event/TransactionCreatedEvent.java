package com.example.payment.domain.event;

import com.example.payment.domain.model.Transaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TransactionCreatedEvent {
    private final Transaction transaction;
} 