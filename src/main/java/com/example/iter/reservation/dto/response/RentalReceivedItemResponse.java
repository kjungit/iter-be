package com.example.iter.reservation.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalReceivedItemResponse(
        Long rentalId,
        Long equipmentId,
        String productNameSnapshot,
        UserSummaryResponse renter,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        PaymentStatus paymentStatus,
        String requestMessage,
        RentalStatus status
) {
    public static RentalReceivedItemResponse of(Rental rental, UserSummaryResponse renter, PaymentStatus paymentStatus) {
        return new RentalReceivedItemResponse(
                rental.getId(),
                rental.getEquipmentId(),
                rental.getProductNameSnapshot(),
                renter,
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getTotalPrice(),
                paymentStatus,
                rental.getRequestMessage(),
                rental.getStatus()
        );
    }
}
