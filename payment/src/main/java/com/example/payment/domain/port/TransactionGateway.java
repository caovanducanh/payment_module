package com.example.payment.domain.port;

import com.example.payment.domain.model.Transaction;

public interface TransactionGateway {
    String provider();
    Transaction execute(Transaction transaction);
}
