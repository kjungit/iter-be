package com.example.iter.device.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipmentEstimateResponse(
        Long equipmentId,
        LocalDate startDate,
        LocalDate endDate,
        int rentalDays,
        BigDecimal dailyPrice,
        BigDecimal totalPrice
) {
}
