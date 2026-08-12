package com.example.iter.reservation.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;

import java.time.LocalDate;

public record ReturnTargetResponse(
        Long rentalId,
        String equipmentName,
        String thumbnailUrl,
        UserSummaryResponse renter,
        LocalDate endDate,
        LocalDate returnDate
) {
}