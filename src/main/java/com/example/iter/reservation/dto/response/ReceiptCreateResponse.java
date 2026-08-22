package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.time.LocalDateTime;

public record ReceiptCreateResponse(
        Long rentalId,
        RentalStatus status,
        LocalDateTime receivedAt
) {
}
