package com.example.iter.reservation.dto.response;

import java.math.BigDecimal;

public record RentalEquipmentSnapshotResponse(
        Long equipmentId,
        String equipmentName,
        String category,
        BigDecimal dailyPrice,
        String thumbnailUrl
) {
}
