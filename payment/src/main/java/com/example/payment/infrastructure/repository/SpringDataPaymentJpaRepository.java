package com.example.payment.infrastructure.repository;

import com.example.payment.infrastructure.entity.TransactionEntity;
import com.example.payment.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentJpaRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findByOrderId(String orderId);
    List<TransactionEntity> findByStatusAndExpiryTimeBefore(TransactionStatus status, LocalDateTime expiryTime);
}