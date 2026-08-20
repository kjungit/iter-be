package com.example.iter.payment.dto.response;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPaymentRentalResponse(
        Long equipmentId,
        String equipmentName,
        String category,
        BigDecimal dailyPrice,
        LocalDate startDate,
        LocalDate endDate,
        Integer rentalDays,
        BigDecimal totalPrice,
        RentalStatus rentalStatus
) {
}
