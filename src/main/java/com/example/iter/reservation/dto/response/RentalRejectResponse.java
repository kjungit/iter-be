package com.example.iter.reservation.dto.response;

import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

public record RentalRejectResponse(
        Long rentalId,
        RentalStatus status,
        String reason,
        PaymentStatus paymentStatus
) {
    public static RentalRejectResponse of(Rental rental, PaymentStatus paymentStatus) {
        return new RentalRejectResponse(rental.getId(), rental.getStatus(), rental.getRejectReason(), paymentStatus);
    }
}
