package com.example.payment.domain.port;

import com.example.payment.domain.model.PaymentResult;
import com.example.payment.domain.model.RefundResult;
import com.example.payment.domain.model.RefundRequest;
import com.example.payment.domain.model.PaymentGatewayType;
import com.example.payment.domain.model.PaymentMethod;
import com.example.payment.domain.model.Transaction;
import java.util.Set;

public interface PaymentGateway {
    PaymentResult initiatePayment(Transaction transaction);
    PaymentResult verifyPayment(String transactionId);
    RefundResult processRefund(RefundRequest request);
    PaymentGatewayType getType();
    Set<PaymentMethod> getSupportedMethods();
    default boolean supports(PaymentMethod method) {
        return getSupportedMethods().contains(method);
    }
} 