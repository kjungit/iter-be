package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.ProductConditionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipmentUpdateRequest(
        @Size(max = 100, message = "장비명은 100자 이하여야 합니다.")
        String name,
        String description,
        @Positive(message = "1일 대여 가격은 0보다 커야 합니다.")
        BigDecimal dailyPrice,
        @FutureOrPresent(message = "대여 가능 시작일은 현재 날짜 이상이어야 합니다.")
        LocalDate availableFrom,
        LocalDate availableTo,
        ProductConditionType productCondition,
        String conditionDetail
) {

    @AssertTrue(message = "전달된 문자열은 빈 값일 수 없습니다.")
    public boolean isValidStrings() {
        return (name == null || !name.isBlank())
                && (description == null || !description.isBlank())
                && (conditionDetail == null || !conditionDetail.isBlank());
    }

    @AssertTrue(message = "변경할 필드를 하나 이상 입력해주세요.")
    public boolean hasChanges() {
        return name != null
                || description != null
                || dailyPrice != null
                || availableFrom != null
                || availableTo != null
                || productCondition != null
                || conditionDetail != null;
    }
}
