package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.time.LocalDateTime;

public record ShippingRegisterResponse(
        Long rentalId,
        RentalStatus status,
        String carrier,
        String trackingNumber,
        LocalDateTime shippedAt
) {
}
