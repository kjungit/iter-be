package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalCreateResponse(
        Long rentalId,
        Long equipmentId,
        RentalStatus status,
        String productNameSnapshot,
        String categorySnapshot,
        BigDecimal dailyPriceSnapshot,
        LocalDate startDate,
        LocalDate endDate,
        int rentalDays,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {
    public static RentalCreateResponse from(Rental rental) {
        return new RentalCreateResponse(
                rental.getId(),
                rental.getEquipmentId(),
                rental.getStatus(),
                rental.getProductNameSnapshot(),
                rental.getCategorySnapshot(),
                rental.getDailyPriceSnapshot(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getRentalDays(),
                rental.getTotalPrice(),
                rental.getCreatedAt()
        );
    }
}
