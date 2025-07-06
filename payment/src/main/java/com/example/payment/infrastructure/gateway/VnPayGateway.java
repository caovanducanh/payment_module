package com.example.payment.infrastructure.gateway;

import com.example.payment.domain.model.Transaction;
import com.example.payment.domain.port.TransactionGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class VnPayGateway implements TransactionGateway {

    private static final Logger logger = LoggerFactory.getLogger(VnPayGateway.class);

    private final VnPayApiClient apiClient;

    public VnPayGateway(VnPayApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String provider() {
        return "VNPAY";
    }

    @Override
    public Transaction execute(Transaction transaction) {
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

            // ✅ Phải set lại cả redirectUrl và expiryTime
            transaction.setRedirectUrl(paymentUrl);
            transaction.setExpiryTime(expiryTime);
            return transaction;

        } catch (Exception e) {
            logger.error("Error generating VNPAY URL", e);
            throw new RuntimeException("Failed to generate VNPAY URL", e);
        }
    }

}
