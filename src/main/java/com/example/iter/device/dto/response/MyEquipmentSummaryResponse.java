package com.example.iter.device.dto.response;

import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MyEquipmentSummaryResponse(
        Long id,
        String name,
        EquipmentCategory category,
        BigDecimal dailyPrice,
        EquipmentStatus status,
        ProductConditionType productCondition,
        String thumbnailUrl,
        LocalDate availableFrom,
        LocalDate availableTo
) {
}
