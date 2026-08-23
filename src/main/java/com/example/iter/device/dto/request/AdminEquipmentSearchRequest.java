package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminEquipmentSearchRequest(
        @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
        String keyword,

        @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
        String category,

        EquipmentStatus status,

        @Size(max = 200, message = "커서는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public AdminEquipmentSearchRequest {
        size = size == null ? 20 : size;
    }
}
