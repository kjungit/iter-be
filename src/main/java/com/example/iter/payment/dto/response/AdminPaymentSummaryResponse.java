package com.example.iter.payment.dto.response;

import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentSummaryResponse(
        Long paymentId,
        Long rentalId,
        String orderId,
        Long renterId,
        String renterEmail,
        String renterName,
        String renterNickname,
        String equipmentName,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        RentalStatus rentalStatus,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt
) {
}
