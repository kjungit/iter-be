package com.example.iter.device.dto.response;

import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EquipmentDetailResponse(
        Long id,
        String name,
        EquipmentCategory category,
        String description,
        BigDecimal dailyPrice,
        LocalDate availableFrom,
        LocalDate availableTo,
        EquipmentStatus status,
        ProductConditionType productCondition,
        String conditionDetail,
        List<EquipmentImageResponse> images,
        EquipmentOwnerResponse owner,
        double averageRating,
        long reviewCount,
        LocalDateTime createdAt
) {
    public EquipmentDetailResponse {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
