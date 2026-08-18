package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipmentSearchRequest(
        @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
        String keyword,

        EquipmentCategory category,

        @DecimalMin(value = "0", message = "최소 가격은 0 이상이어야 합니다.")
        BigDecimal minPrice,

        @DecimalMin(value = "0", message = "최대 가격은 0 이상이어야 합니다.")
        BigDecimal maxPrice,

        LocalDate startDate,
        LocalDate endDate,
        EquipmentSort sort,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public EquipmentSearchRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        sort = sort == null ? EquipmentSort.LATEST : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    @AssertTrue(message = "최소 가격은 최대 가격보다 클 수 없습니다.")
    public boolean isValidPriceRange() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    @AssertTrue(message = "대여 시작일과 종료일을 함께 올바르게 입력해주세요.")
    public boolean isValidRentalPeriod() {
        if (startDate == null && endDate == null) {
            return true;
        }
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
}
