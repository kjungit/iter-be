package com.example.iter.payment.dto.response;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long rentalId,
        Long paymentId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        LocalDateTime paidAt,
        RentalStatus rentalStatus,
        BigDecimal pointBalance
) {
    public static PaymentResponse of(Rental rental, Payment payment, BigDecimal pointBalance) {
        return new PaymentResponse(
                rental.getId(),
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                rental.getStatus(),
                pointBalance
        );
    }
}
