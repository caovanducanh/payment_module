package com.example.payment.domain.port;

import com.example.payment.domain.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> saveAll(Iterable<Transaction> transactions);
    Optional<Transaction> findByOrderId(String orderId);
    List<Transaction> findByStatusAndExpiryTimeBefore(String status, LocalDateTime expiryTime);
}