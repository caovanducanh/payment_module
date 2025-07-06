package com.example.payment.infrastructure.repository;

import com.example.payment.application.mapper.TransactionMapper;
import com.example.payment.domain.model.Transaction;
import com.example.payment.domain.port.TransactionRepository;
import com.example.payment.infrastructure.entity.TransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final SpringDataPaymentJpaRepository jpa;
    private final TransactionMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = mapper.toEntity(transaction);
        TransactionEntity savedEntity = jpa.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<Transaction> saveAll(Iterable<Transaction> transactions) {
        List<TransactionEntity> entities = mapper.toEntities(transactions);
        List<TransactionEntity> savedEntities = jpa.saveAll(entities);
        return mapper.toDomains(savedEntities);
    }

    @Override
    public Optional<Transaction> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Transaction> findByStatusAndExpiryTimeBefore(String status, LocalDateTime expiryTime) {
        return jpa.findByStatusAndExpiryTimeBefore(status, expiryTime).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}