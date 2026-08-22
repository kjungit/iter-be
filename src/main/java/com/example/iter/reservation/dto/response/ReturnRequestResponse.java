package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.RentalStatus;

public record ReturnRequestResponse(
        Long rentalId,
        RentalStatus status
) {
}
