package com.example.iter.device.dto.response;

import java.time.LocalDate;

public record EquipmentAvailabilityResponse(
        Long equipmentId,
        LocalDate startDate,
        LocalDate endDate,
        boolean available,
        AvailabilityReason reason
) {
}
