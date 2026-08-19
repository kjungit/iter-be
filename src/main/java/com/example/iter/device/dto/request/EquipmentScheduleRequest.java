package com.example.iter.device.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EquipmentScheduleRequest(
        @NotNull(message = "조회 시작일은 필수입니다.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @NotNull(message = "조회 종료일은 필수입니다.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
) {

    @AssertTrue(message = "조회 시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isValidPeriod() {
        return from == null || to == null || !from.isAfter(to);
    }
}
