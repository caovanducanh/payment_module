package com.example.payment.application.service;

import com.example.payment.application.mapper.TransactionMapper;
import com.example.payment.application.usecase.PaymentUseCase;
import com.example.payment.domain.event.TransactionCompletedEvent;
import com.example.payment.domain.model.Transaction;
import com.example.payment.domain.port.TransactionGateway;
import com.example.payment.domain.port.TransactionRepository;
import com.example.payment.web.dto.request.PaymentRequest;
import com.example.payment.web.dto.response.PaymentResponse;
import com.example.payment.web.dto.response.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionServiceImpl implements PaymentUseCase {

    private final Map<String, TransactionGateway> gatewayMap;
    private final TransactionMapper mapper;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final String VNP_SECRET_KEY = "T81UQ58X7Q54PYBVEO6FJ4AKQSUAP1S2";

    public TransactionServiceImpl(List<TransactionGateway> gateways,
                                  TransactionMapper mapper,
                                  TransactionRepository transactionRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(TransactionGateway::provider, g -> g));
        this.mapper = mapper;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ResponseObject handle(PaymentRequest request) {
        transactionRepository.findByOrderId(request.getOrderId())
                .ifPresent(existing -> {
                    if (!"PENDING".equals(existing.getStatus())) {
                        throw new IllegalStateException("Transaction with orderId " + request.getOrderId() +
                                " already exists with status " + existing.getStatus());
                    }
                });

        TransactionGateway gateway = gatewayMap.get(request.getProvider());
        if (gateway == null) {
            return buildErrorResponse("Unsupported provider: " + request.getProvider(),
                    HttpStatus.BAD_REQUEST, null);
        }

        try {
            Transaction transaction = mapper.toDomain(request);
            transaction.setExpiryTime(LocalDateTime.now().plusMinutes(1));

            Transaction result = gateway.execute(transaction);
            Transaction saved = transactionRepository.save(result);
            eventPublisher.publishEvent(new TransactionCompletedEvent(saved));

            return buildSuccessResponse("Payment URL created successfully", mapper.toResponse(saved));

        } catch (Exception e) {
            return buildErrorResponse("Failed to generate payment URL: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    @Override
    @Transactional
    public ResponseObject handleVNPayReturn(Map<String, String> params) {
        return handleVNPayCallback(params, false);
    }

    @Override
    @Transactional
    public ResponseObject handleVNPayIPN(Map<String, String> params) {
        return handleVNPayCallback(params, true);
    }

    @Transactional
    public ResponseObject handleVNPayCallback(Map<String, String> params, boolean isIPN) {
        final String[] REQUIRED_PARAMS = {"vnp_SecureHash", "vnp_TxnRef", "vnp_ResponseCode"};

        for (String param : REQUIRED_PARAMS) {
            if (!params.containsKey(param)) {
                return buildErrorResponse("Missing required parameter: " + param,
                        HttpStatus.BAD_REQUEST, Map.of("RspCode", "96"));
            }
        }

        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        try {
            Transaction transaction = transactionRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if (List.of("CANCELLED", "EXPIRED", "PAID").contains(transaction.getStatus())) {
                return buildErrorResponse("Transaction already processed",
                        HttpStatus.BAD_REQUEST,
                        Map.of("RspCode", "02", "CurrentStatus", transaction.getStatus()));
            }

            if (isTransactionExpired(transaction)) {
                log.warn("Transaction {} expired at {}", orderId, transaction.getExpiryTime());
                transaction.setStatus("EXPIRED");
                transactionRepository.save(transaction);
                return buildErrorResponse("Transaction expired",
                        HttpStatus.BAD_REQUEST,
                        Map.of("RspCode", "70"));
            }

            if ("00".equals(responseCode)) {
                return handleSuccessfulPayment(transaction, params.get("vnp_TransactionNo"), isIPN);
            } else {
                return handleFailedPayment(transaction, responseCode, isIPN);
            }

        } catch (Exception e) {
            return buildErrorResponse("Processing error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    Map.of("RspCode", "99"));
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processExpiredTransactions() {
        List<Transaction> expiredTransactions = transactionRepository
                .findByStatusAndExpiryTimeBefore("PENDING", LocalDateTime.now());

        if (!expiredTransactions.isEmpty()) {
            expiredTransactions.forEach(transaction -> {
                log.warn("Marking transaction {} as EXPIRED (was pending since {})",
                        transaction.getOrderId(), transaction.getExpiryTime());
                transaction.setStatus("EXPIRED");
            });
            transactionRepository.saveAll(expiredTransactions);
        }
    }

    @Override
    @Transactional
    public ResponseObject cancelPendingVNPay(String orderId) {
        try {
            Transaction transaction = transactionRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if (!List.of("PENDING", "FAILED", "EXPIRED").contains(transaction.getStatus())) {
                return buildErrorResponse("Only PENDING, FAILED or EXPIRED transactions can be cancelled",
                        HttpStatus.BAD_REQUEST, null);
            }

            transaction.setStatus("CANCELLED");
            transactionRepository.save(transaction);
            return buildSuccessResponse("Transaction cancelled successfully", null);

        } catch (Exception e) {
            return buildErrorResponse("Error cancelling transaction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    private boolean isTransactionExpired(Transaction transaction) {
        return transaction.getExpiryTime() != null &&
                transaction.getExpiryTime().isBefore(LocalDateTime.now()) &&
                "PENDING".equals(transaction.getStatus());
    }

    private ResponseObject handleSuccessfulPayment(Transaction transaction, String transactionId, boolean isIPN) {
        if (!"PAID".equals(transaction.getStatus())) {
            transaction.markAsPaid(transactionId);
            transactionRepository.save(transaction);
            eventPublisher.publishEvent(new TransactionCompletedEvent(transaction));
        }
        return buildSuccessResponse("Payment confirmed",
                Map.of("RspCode", "00", "TransactionId", transactionId));
    }

    private ResponseObject handleFailedPayment(Transaction transaction, String responseCode, boolean isIPN) {
        if ("PENDING".equals(transaction.getStatus())) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
        }
        return buildErrorResponse("Payment failed with code: " + responseCode,
                HttpStatus.BAD_REQUEST,
                Map.of("RspCode", "99", "VnpResponseCode", responseCode));
    }

    private ResponseObject buildSuccessResponse(String message, Object data) {
        return ResponseObject.builder()
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }

    private ResponseObject buildErrorResponse(String message, HttpStatus status, Object data) {
        return ResponseObject.builder()
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private String maskSecretKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }
}