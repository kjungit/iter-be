package com.example.iter.device.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminEquipmentSummaryResponse(
        Long equipmentId,
        String name,
        EquipmentCategory category,
        BigDecimal dailyPrice,
        EquipmentStatus status,
        UserSummaryResponse owner,
        String thumbnailUrl,
        LocalDateTime createdAt
) {

}
