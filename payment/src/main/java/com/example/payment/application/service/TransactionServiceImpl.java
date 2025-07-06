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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        TransactionGateway gateway = gatewayMap.get(request.getProvider());
        if (gateway == null) {
            return buildErrorResponse("Unsupported provider: " + request.getProvider(),
                    HttpStatus.BAD_REQUEST, null);
        }

        try {
            Transaction transaction = mapper.toDomain(request);
            Transaction result = gateway.execute(transaction);

            log.info("Saving transaction with orderId: {}", transaction.getOrderId());
            Transaction saved = transactionRepository.save(result);
            log.info("Saved transaction: {}", saved.getOrderId());

            eventPublisher.publishEvent(new TransactionCompletedEvent(saved));
            PaymentResponse response = mapper.toResponse(saved);

            return buildSuccessResponse("Payment URL created successfully", response);

        } catch (Exception e) {
            log.error("Failed to generate payment URL", e);
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
        final String prefix = isIPN ? "IPN: " : "";
        final String[] REQUIRED_PARAMS = {"vnp_SecureHash", "vnp_TxnRef", "vnp_ResponseCode"};

        // 1. Validate required parameters
        for (String param : REQUIRED_PARAMS) {
            if (!params.containsKey(param)) {
                log.error("{}Missing required parameter: {}", prefix, param);
                return buildErrorResponse(prefix + "Missing required parameter: " + param,
                        HttpStatus.BAD_REQUEST, Map.of("RspCode", "96"));
            }
        }

        String secureHash = params.get("vnp_SecureHash");
        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionId = params.get("vnp_TransactionNo");

        try {
            // 2. Prepare parameters - giữ nguyên case của key như VNPay gửi về
            Map<String, String> filteredParams = new LinkedHashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith("vnp_") &&
                        !key.equalsIgnoreCase("vnp_SecureHash") &&
                        !key.equalsIgnoreCase("vnp_SecureHashType")) {
                    filteredParams.put(key, value);
                }
            });

            // 3. Sort parameters exactly like VNPay does
            Map<String, String> sortedParams = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sortedParams.putAll(filteredParams);

            // 4. Build sign data - không encode giá trị
            StringBuilder signData = new StringBuilder();
            sortedParams.forEach((key, value) -> {
                if (signData.length() > 0) signData.append("&");
                signData.append(key).append("=").append(value);
            });

            // 5. Generate hash với secret key CHÍNH XÁC
            String calculatedHash = hmacSHA512(VNP_SECRET_KEY, signData.toString());

            // Debug logs - ghi đầy đủ thông tin
            log.info("{}====== VNPay Callback Debug ======", prefix);
            log.info("{}Order ID: {}", prefix, orderId);
            log.info("{}All received params: {}", prefix, params);
            log.info("{}Parameters for signing: {}", prefix, sortedParams);
            log.info("{}Sign data: {}", prefix, signData);
            log.info("{}Calculated hash: {}", prefix, calculatedHash);
            log.info("{}Received hash: {}", prefix, secureHash);

            // 6. Verify hash - so sánh chính xác
            if (!calculatedHash.equalsIgnoreCase(secureHash)) {
                log.error("{}Hash verification failed for order: {}", prefix, orderId);
                log.error("{}Expected: {}", prefix, calculatedHash);
                log.error("{}Received: {}", prefix, secureHash);

                // Thêm cơ chế fallback - thử lại với cách sắp xếp khác nếu cần
                String fallbackHash = tryAlternativeHashingMethods(params);
                if (fallbackHash.equalsIgnoreCase(secureHash)) {
                    log.warn("{}Hash matched using fallback method!", prefix);
                } else {
                    return buildErrorResponse(prefix + "Invalid VNPAY signature",
                            HttpStatus.BAD_REQUEST,
                            Map.of(
                                    "RspCode", "97",
                                    "DebugInfo", Map.of(
                                            "SignMethod", "HMAC-SHA512",
                                            "SignData", signData.toString(),
                                            "ExpectedHash", calculatedHash,
                                            "ReceivedHash", secureHash,
                                            "FallbackHash", fallbackHash,
                                            "SecretKey", maskSecretKey(VNP_SECRET_KEY)
                                    )
                            ));
                }
            }

            // 7. Process transaction
            Transaction transaction = transactionRepository.findByOrderId(orderId)
                    .orElseThrow(() -> {
                        log.error("{}Transaction not found: {}", prefix, orderId);
                        return new RuntimeException(prefix + "Transaction not found");
                    });

            // 8. Check expiry
            if (isTransactionExpired(transaction)) {
                transaction.setStatus("EXPIRED");
                transactionRepository.save(transaction);
                log.warn("{}Transaction expired: {}", prefix, orderId);
                return buildErrorResponse(prefix + "Transaction expired",
                        HttpStatus.BAD_REQUEST,
                        Map.of("RspCode", "70"));
            }

            // 9. Handle payment result
            if ("00".equals(responseCode)) {
                return handleSuccessfulPayment(transaction, transactionId, isIPN);
            } else {
                return handleFailedPayment(transaction, responseCode, isIPN);
            }

        } catch (Exception e) {
            log.error("{}Error processing callback for order {}: {}", prefix, orderId, e.getMessage(), e);
            return buildErrorResponse(prefix + "Processing error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    Map.of("RspCode", "99"));
        }
    }

// ========== HELPER METHODS ==========

    private String tryAlternativeHashingMethods(Map<String, String> params) {
        // Thử các phương pháp hash thay thế nếu cần
        try {
            // Cách 1: Không sắp xếp tham số
            StringBuilder rawSignData = new StringBuilder();
            params.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("vnp_"))
                    .filter(e -> !e.getKey().equalsIgnoreCase("vnp_SecureHash"))
                    .forEach(e -> {
                        if (rawSignData.length() > 0) rawSignData.append("&");
                        rawSignData.append(e.getKey()).append("=").append(e.getValue());
                    });
            String hash1 = hmacSHA512(VNP_SECRET_KEY, rawSignData.toString());

            // Cách 2: Sắp xếp theo thứ tự khác
            Map<String, String> sortedParams = new TreeMap<>(params);
            String sortedSignData = sortedParams.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("vnp_"))
                    .filter(e -> !e.getKey().equalsIgnoreCase("vnp_SecureHash"))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            String hash2 = hmacSHA512(VNP_SECRET_KEY, sortedSignData);

            return hash2; // Ưu tiên trả về hash từ cách 2
        } catch (Exception e) {
            return "fallback-hash-error";
        }
    }

    private String maskSecretKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
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
            log.error("Error cancelling transaction {}: {}", orderId, e.getMessage(), e);
            return buildErrorResponse("Error cancelling transaction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // ========== HELPER METHODS ==========

    private boolean isTransactionExpired(Transaction transaction) {
        return transaction.getExpiryTime() != null &&
                transaction.getExpiryTime().isBefore(LocalDateTime.now()) &&
                "PENDING".equals(transaction.getStatus());
    }

    private ResponseObject handleSuccessfulPayment(Transaction transaction, String transactionId, boolean isIPN) {
        String prefix = isIPN ? "IPN: " : "";

        if (!"PAID".equals(transaction.getStatus())) {
            transaction.markAsPaid(transactionId);
            transactionRepository.save(transaction);
            eventPublisher.publishEvent(new TransactionCompletedEvent(transaction));
            log.info("{}Payment processed for order: {}", prefix, transaction.getOrderId());
        }

        return buildSuccessResponse(prefix + "Payment confirmed",
                Map.of(
                        "RspCode", "00",
                        "TransactionId", transactionId,
                        "Amount", transaction.getAmount(),
                        "OrderId", transaction.getOrderId()
                ));
    }

    private ResponseObject handleFailedPayment(Transaction transaction, String responseCode, boolean isIPN) {
        String prefix = isIPN ? "IPN: " : "";

        if ("PENDING".equals(transaction.getStatus())) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            log.warn("{}Payment failed for order: {}", prefix, transaction.getOrderId());
        }

        return buildErrorResponse(prefix + "Payment failed with code: " + responseCode,
                HttpStatus.BAD_REQUEST,
                Map.of(
                        "RspCode", "99",
                        "VnpResponseCode", responseCode
                ));
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
        // 1. Chuyển key và data sang byte array với encoding UTF-8
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // 2. Khởi tạo HMAC-SHA512
        Mac hmac = Mac.getInstance("HmacSHA512");
        hmac.init(new SecretKeySpec(keyBytes, "HmacSHA512"));

        // 3. Tính toán hash
        byte[] hashBytes = hmac.doFinal(dataBytes);

        // 4. Chuyển hash sang hex (viết thường)
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}