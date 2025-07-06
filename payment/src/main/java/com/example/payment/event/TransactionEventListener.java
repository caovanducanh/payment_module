package com.example.payment.event;

import com.example.payment.domain.event.TransactionCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {
    @EventListener
    public void handlePaymentComplete(TransactionCompletedEvent event) {
        System.out.println("✅ Gửi mail xác nhận thanh toán cho đơn hàng: " +
                event.getTransaction().getOrderId());
    }
}