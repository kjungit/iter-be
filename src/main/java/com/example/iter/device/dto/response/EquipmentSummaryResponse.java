package com.example.iter.device.dto.response;

import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.ProductConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipmentSummaryResponse(
        Long id,
        String name,
        EquipmentCategory category,
        BigDecimal dailyPrice,
        LocalDate availableFrom,
        LocalDate availableTo,
        ProductConditionType productCondition,
        String thumbnailUrl,
        double averageRating,
        long reviewCount
) {
}
