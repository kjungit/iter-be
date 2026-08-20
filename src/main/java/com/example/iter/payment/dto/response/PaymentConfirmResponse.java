package com.example.iter.payment.dto.response;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentConfirmResponse(
        Long rentalId,
        Long paymentId,
        String paymentKey,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        LocalDateTime paidAt,
        RentalStatus rentalStatus
) {
    public static PaymentConfirmResponse of( Rental rental, Payment payment ) {
        return new PaymentConfirmResponse(
                rental.getId(),
                payment.getId(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                rental.getStatus()
        );
    }
}