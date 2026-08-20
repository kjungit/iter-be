package com.example.iter.device.dto.response;

import java.time.LocalDate;
import java.util.List;

public record EquipmentScheduleResponse(
        Long equipmentId,
        LocalDate from,
        LocalDate to,
        List<RentalScheduleItemResponse> rentals
) {
    public EquipmentScheduleResponse {
        rentals = List.copyOf(rentals);
    }
}
