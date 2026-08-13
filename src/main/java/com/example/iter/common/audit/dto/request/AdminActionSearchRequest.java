package com.example.iter.common.audit.dto.request;

import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record AdminActionSearchRequest(
        AdminActionTargetType targetType,

        @Positive(message = "대상 ID는 양수여야 합니다.")
        Long targetId,

        AdminActionType action,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public AdminActionSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
