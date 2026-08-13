package com.example.iter.reservation.dto.response;

import com.example.iter.device.domain.entity.Equipment;

import java.math.BigDecimal;

public record RentalEquipmentSnapshotResponse(
        Long equipmentId,
        String equipmentName,
        String category,
        BigDecimal dailyPrice,
        String thumbnailUrl
) {
    // TODO: 장비 썸네일 URL — EquipmentImage 연동 필요 (A 담당 영역), 우선 null
    public static RentalEquipmentSnapshotResponse from(Equipment equipment) {
        return new RentalEquipmentSnapshotResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDailyPrice(),
                null
        );
    }
}
