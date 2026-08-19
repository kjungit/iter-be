package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.time.LocalDateTime;

public record RentalApproveResponse(
        Long rentalId,
        RentalStatus status,
        LocalDateTime approvedAt
) {
    public static RentalApproveResponse from(Rental rental) {
        return new RentalApproveResponse(rental.getId(), rental.getStatus(), rental.getApprovedAt());
    }
}
