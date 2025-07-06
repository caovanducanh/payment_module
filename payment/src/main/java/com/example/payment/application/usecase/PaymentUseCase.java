package com.example.payment.application.usecase;

import com.example.payment.web.dto.request.PaymentRequest;
import com.example.payment.web.dto.response.PaymentResponse;
import com.example.payment.web.dto.response.ResponseObject;

import java.util.Map;

public interface PaymentUseCase {
    ResponseObject handle(PaymentRequest request);
    ResponseObject handleVNPayReturn(Map<String, String> params);
    ResponseObject handleVNPayIPN(Map<String, String> params);

    ResponseObject cancelPendingVNPay(String orderId);
}
