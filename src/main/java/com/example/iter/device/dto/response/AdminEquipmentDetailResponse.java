package com.example.iter.device.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminEquipmentDetailResponse(
        Long equipmentId,
        UserSummaryResponse owner,
        String category,
        String name,
        String description,
        BigDecimal dailyPrice,
        LocalDate availableFrom,
        LocalDate availableTo,
        EquipmentStatus status,
        ProductConditionType productCondition,
        String conditionDetail,
        List<ImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AdminEquipmentDetailResponse {
        images = images == null
                ? List.of()
                : List.copyOf(images);
    }
}
