package com.example.iter.reservation.dto.request;

import com.example.iter.reservation.domain.entity.RentalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RentalHistorySearchRequest(
        RentalStatus status,

        @Size(max = 100, message = "장비명은 100자 이하여야 합니다.")
        String equipmentName,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public RentalHistorySearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
