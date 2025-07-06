package com.example.payment.web.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private String orderId;
    private double amount;
    private String status;
    private String provider;
    private String transactionId;
    private String redirectUrl;
}
