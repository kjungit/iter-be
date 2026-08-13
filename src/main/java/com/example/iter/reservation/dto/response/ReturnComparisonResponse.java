package com.example.iter.reservation.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;

import java.time.LocalDate;

public record ReturnComparisonResponse(
        Long rentalId,
        String equipmentName,
        UserSummaryResponse renter,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate returnDate,
        ConditionEvidenceResponse receipt,
        ConditionEvidenceResponse returnReceipt
) {
}
