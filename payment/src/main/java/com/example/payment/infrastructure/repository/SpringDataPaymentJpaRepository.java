package com.example.payment.infrastructure.repository;

import com.example.payment.infrastructure.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentJpaRepository extends JpaRepository<TransactionEntity, String> {
}
