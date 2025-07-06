package com.example.payment.domain.event;

import com.example.payment.domain.model.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionCompletedEvent extends ApplicationEvent {
    private final Transaction transaction;

    public TransactionCompletedEvent(Transaction transaction) {
        super(transaction);
        this.transaction = transaction;
    }

}