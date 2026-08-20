package com.example.iter.payment.dto.toss;

// POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel 요청 바디
public record TossCancelApiRequest(String cancelReason) {
}
