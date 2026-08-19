package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.ProductConditionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public record EquipmentCreateRequest(
        @NotNull(message = "장비 카테고리는 필수입니다.")
        EquipmentCategory category,

        @NotBlank(message = "장비명은 필수입니다.")
        @Size(max = 100, message = "장비명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "장비 설명은 필수입니다.")
        String description,

        @NotNull(message = "1일 대여 가격은 필수입니다.")
        @Positive(message = "1일 대여 가격은 0보다 커야 합니다.")
        BigDecimal dailyPrice,

        @NotNull(message = "대여 가능 시작일은 필수입니다.")
        @FutureOrPresent(message = "대여 가능 시작일은 현재 날짜 이상이어야 합니다.")
        LocalDate availableFrom,

        @NotNull(message = "대여 가능 종료일은 필수입니다.")
        LocalDate availableTo,

        @NotNull(message = "장비 컨디션은 필수입니다.")
        ProductConditionType productCondition,

        String conditionDetail,

        @NotEmpty(message = "장비 이미지는 한 장 이상 필요합니다.")
        @Size(max = 5, message = "장비 이미지는 5장 이하로 등록해주세요.")
        List<@NotBlank(message = "이미지 객체 키는 빈 값일 수 없습니다.") String> imageKeys,

        @Min(value = 0, message = "대표 이미지 인덱스는 0 이상이어야 합니다.")
        int thumbnailIndex
) {

    @AssertTrue(message = "대여 가능 종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isValidAvailablePeriod() {
        return availableFrom == null || availableTo == null || !availableTo.isBefore(availableFrom);
    }

    @AssertTrue(message = "NORMAL이 아닌 장비는 컨디션 상세 설명이 필수입니다.")
    public boolean isValidConditionDetail() {
        return productCondition == null
                || productCondition == ProductConditionType.NORMAL
                || conditionDetail != null && !conditionDetail.isBlank();
    }

    @AssertTrue(message = "대표 이미지 인덱스가 이미지 목록 범위를 벗어났습니다.")
    public boolean isValidThumbnailIndex() {
        return imageKeys == null
                || imageKeys.isEmpty()
                || thumbnailIndex < imageKeys.size();
    }

    @AssertTrue(message = "중복된 이미지 객체 키는 등록할 수 없습니다.")
    public boolean hasNoDuplicateImageKeys() {
        return imageKeys == null
                || new HashSet<>(imageKeys).size() == imageKeys.size();
    }
}
