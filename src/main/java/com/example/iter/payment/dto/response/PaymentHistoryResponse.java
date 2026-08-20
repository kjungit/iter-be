package com.example.iter.payment.dto.response;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long paymentId,
        Long rentalId,
        Long equipmentId,
        String equipmentName,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        String orderId,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt
) {
    public static PaymentHistoryResponse of(Payment payment, Rental rental) {
        return new PaymentHistoryResponse(
                payment.getId(),
                rental.getId(),
                rental.getEquipmentId(),
                rental.getProductNameSnapshot(),
                rental.getStartDate(),
                rental.getEndDate(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getOrderId(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt()
        );
    }
}
