package com.example.payment.infrastructure.gateway;

import com.example.payment.domain.model.*;
import com.example.payment.domain.port.PaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

@Component
public class VnPayGateway implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(VnPayGateway.class);

    private final VnPayApiClient apiClient;

    public VnPayGateway(VnPayApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    @CircuitBreaker(name = "vnpayGateway", fallbackMethod = "fallbackPayment")
    @Retry(name = "vnpayGateway")
    public PaymentResult initiatePayment(Transaction transaction) {
        try {
            logger.info("Generating VNPAY URL for order: {}", transaction.getOrderId());

            LocalDateTime expiryTime = transaction.getExpiryTime() != null
                    ? transaction.getExpiryTime()
                    : LocalDateTime.now().plusMinutes(15);

            String paymentUrl = apiClient.generatePaymentUrl(
                    transaction.getOrderId(),
                    BigDecimal.valueOf(transaction.getAmount()),
                    expiryTime
            );

            transaction.setRedirectUrl(paymentUrl);
            transaction.setExpiryTime(expiryTime);
            return new PaymentResult(true, "Payment URL generated", transaction.getOrderId(), TransactionStatus.PENDING);
        } catch (Exception e) {
            logger.error("Error generating VNPAY URL", e);
            return new PaymentResult(false, "Failed to generate VNPAY URL: " + e.getMessage(), null, TransactionStatus.FAILED);
        }
    }

    public PaymentResult fallbackPayment(Transaction transaction, Throwable t) {
        logger.error("Fallback triggered for VNPAY payment: {}", t.getMessage());
        return new PaymentResult(false, "VNPAY service unavailable: " + t.getMessage(), null, TransactionStatus.FAILED);
    }

    @Override
    public PaymentResult verifyPayment(String transactionId) {
        // TODO: Implement verify logic with VNPAY API
        return new PaymentResult(true, "Verified (mock)", transactionId, TransactionStatus.PAID);
    }

    @Override
    public RefundResult processRefund(RefundRequest request) {
        // TODO: Implement refund logic with VNPAY API
        return new RefundResult(false, "Refund not supported yet", null, TransactionStatus.FAILED);
    }

    @Override
    public PaymentGatewayType getType() {
        return PaymentGatewayType.VNPAY;
    }

    @Override
    public Set<PaymentMethod> getSupportedMethods() {
        return Collections.singleton(PaymentMethod.VNPAY);
    }
}
