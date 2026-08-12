package com.example.iter.reservation.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalHistoryResponse(
        Long rentalId,
        Long equipmentId,
        String equipmentName,
        String thumbnailUrl,
        UserSummaryResponse counterparty,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        RentalStatus status,
        int overdueDays
) {
}
