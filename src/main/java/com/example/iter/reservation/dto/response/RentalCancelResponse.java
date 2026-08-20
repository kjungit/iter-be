package com.example.iter.reservation.dto.response;

import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

public record RentalCancelResponse(
        Long rentalId,
        RentalStatus status,
        PaymentStatus paymentStatus
) {
    public static RentalCancelResponse of(Rental rental, PaymentStatus paymentStatus) {
        return new RentalCancelResponse(rental.getId(), rental.getStatus(), paymentStatus);
    }
}
