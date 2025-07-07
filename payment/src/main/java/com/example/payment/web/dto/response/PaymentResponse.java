package com.example.payment.web.dto.response;

import com.example.payment.domain.model.TransactionStatus;
import com.example.payment.domain.model.PaymentGatewayType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private String orderId;
    private double amount;
    private TransactionStatus status;
    private PaymentGatewayType provider;
    private String transactionId;
    private String redirectUrl;
}
