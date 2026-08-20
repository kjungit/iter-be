package com.example.iter.device.service.model;

import com.example.iter.device.domain.entity.Equipment;

public record EquipmentDetailRow(
        Equipment equipment,
        Double averageRating,
        Long reviewCount
) {
}
