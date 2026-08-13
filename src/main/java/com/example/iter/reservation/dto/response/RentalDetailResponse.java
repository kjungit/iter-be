package com.example.iter.reservation.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalDetailResponse(
        Long rentalId,
        RentalEquipmentSnapshotResponse equipment,
        UserSummaryResponse owner,
        UserSummaryResponse renter,
        LocalDate startDate,
        LocalDate endDate,
        int rentalDays,
        BigDecimal totalPrice,
        String requestMessage,
        RentalStatus status,
        int overdueDays,
        LocalDateTime createdAt
) {
}
