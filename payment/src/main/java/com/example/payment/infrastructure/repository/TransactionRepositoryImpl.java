package com.example.payment.infrastructure.repository;

import com.example.payment.application.mapper.TransactionMapper;
import com.example.payment.domain.model.Transaction;
import com.example.payment.domain.port.TransactionRepository;
import com.example.payment.infrastructure.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    private final SpringDataPaymentJpaRepository jpa;
    private final TransactionMapper mapper;

    public TransactionRepositoryImpl(SpringDataPaymentJpaRepository jpa, TransactionMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = mapper.toEntity(transaction);
        TransactionEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findByOrderId(String orderId) {
        return jpa.findById(orderId).map(mapper::toDomain);
    }
}
