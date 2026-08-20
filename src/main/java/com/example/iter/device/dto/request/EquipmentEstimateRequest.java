package com.example.iter.device.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EquipmentEstimateRequest(
        @NotNull(message = "대여 시작일과 종료일을 올바르게 입력해주세요.")
        @Future(message = "대여 시작일과 종료일을 올바르게 입력해주세요.")
        LocalDate startDate,

        @NotNull(message = "대여 시작일과 종료일을 올바르게 입력해주세요.")
        LocalDate endDate
) {
    @AssertTrue(message = "대여 시작일과 종료일을 올바르게 입력해주세요.")
    public boolean isValidRentalPeriod() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
