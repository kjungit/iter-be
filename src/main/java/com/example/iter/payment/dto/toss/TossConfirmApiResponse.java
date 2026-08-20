package com.example.iter.payment.dto.toss;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record TossConfirmApiResponse(String paymentKey, String orderId, String status, String approvedAt, Long totalAmount) {
    public LocalDateTime approvedAtAsLocalDateTime() {
        return OffsetDateTime.parse(approvedAt).toLocalDateTime();
    }
}
