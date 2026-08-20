package com.example.iter.payment.service.model;

import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPaymentDetailRow(
        Long paymentId,
        Long rentalId,
        String orderId,
        Long renterId,
        String renterEmail,
        String renterName,
        String renterNickname,
        Long equipmentId,
        String equipmentName,
        String category,
        BigDecimal dailyPrice,
        LocalDate startDate,
        LocalDate endDate,
        Integer rentalDays,
        BigDecimal totalPrice,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        RentalStatus rentalStatus,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
