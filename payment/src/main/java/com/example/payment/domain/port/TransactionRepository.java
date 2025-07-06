package com.example.payment.domain.port;

import com.example.payment.domain.model.Transaction;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findByOrderId(String orderId);
}
