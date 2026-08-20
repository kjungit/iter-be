package com.example.iter.device.dto.response;

import com.example.iter.device.domain.entity.EquipmentStatus;

import java.time.LocalDateTime;

public record EquipmentStatusResponse(
        Long equipmentId,
        EquipmentStatus status,
        LocalDateTime updatedAt
) {
}
