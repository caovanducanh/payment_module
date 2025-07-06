package com.example.payment.application.mapper;

import com.example.payment.domain.model.Transaction;
import com.example.payment.infrastructure.entity.TransactionEntity;
import com.example.payment.web.dto.request.PaymentRequest;
import com.example.payment.web.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

    public Transaction toDomain(PaymentRequest request) {
        return new Transaction(
                request.getOrderId(),
                request.getAmount(),
                "PENDING",
                request.getProvider(),
                null // transactionId chưa có
        );
    }

    public TransactionEntity toEntity(Transaction domain) {
        TransactionEntity entity = new TransactionEntity();
        entity.setOrderId(domain.getOrderId());
        entity.setAmount(domain.getAmount());
        entity.setStatus(domain.getStatus());
        entity.setProvider(domain.getProvider());
        entity.setTransactionId(domain.getTransactionId());
        entity.setRedirectUrl(domain.getRedirectUrl());
        entity.setExpiryTime(domain.getExpiryTime());
        return entity;
    }

    public Transaction toDomain(TransactionEntity entity) {
        Transaction domain = new Transaction(
                entity.getOrderId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getProvider(),
                entity.getTransactionId()
        );
        domain.setRedirectUrl(entity.getRedirectUrl());
        domain.setExpiryTime(entity.getExpiryTime());
        return domain;
    }

    public PaymentResponse toResponse(Transaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(transaction.getOrderId());
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus());
        response.setProvider(transaction.getProvider());
        response.setTransactionId(transaction.getTransactionId());
        response.setRedirectUrl(transaction.getRedirectUrl());
        return response;
    }
    public List<TransactionEntity> toEntities(Iterable<Transaction> domains) {
        return ((List<Transaction>) domains).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    // Thêm phương thức mới để chuyển đổi danh sách
    public List<Transaction> toDomains(List<TransactionEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
