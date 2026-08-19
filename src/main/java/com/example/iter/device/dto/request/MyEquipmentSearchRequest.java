package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MyEquipmentSearchRequest(
        EquipmentStatus status,
        EquipmentSort sort,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public MyEquipmentSearchRequest {
        sort = sort == null ? EquipmentSort.LATEST : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
