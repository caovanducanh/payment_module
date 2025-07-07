package com.example.payment.web.controller;

import com.example.payment.application.usecase.PaymentUseCase;
import com.example.payment.infrastructure.gateway.VnPayApiClient;
import com.example.payment.web.dto.request.PaymentRequest;
import com.example.payment.web.dto.response.PaymentResponse;
import com.example.payment.web.dto.response.ResponseObject;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
@AllArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentService;


    @PostMapping
    public ResponseEntity<ResponseObject> createPayment(@RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.ok(paymentService.handle(request));
    }


    @GetMapping("/return")
    public ResponseEntity<ResponseObject> handleReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVNPayReturn(params));
    }


    @PostMapping("/ipn")
    public ResponseEntity<ResponseObject> handleIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVNPayIPN(params));
    }

    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<ResponseObject> cancelPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.cancelPendingVNPay(orderId));
    }

}

