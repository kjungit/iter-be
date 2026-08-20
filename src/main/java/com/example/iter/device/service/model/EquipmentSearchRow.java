package com.example.iter.device.service.model;

import com.example.iter.device.domain.entity.Equipment;

public record EquipmentSearchRow(
        Equipment equipment,
        Double averageRating,
        Long reviewCount
) {
}
