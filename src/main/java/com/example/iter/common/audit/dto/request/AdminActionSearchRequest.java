package com.example.iter.common.audit.dto.request;

import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminActionSearchRequest(
        AdminActionTargetType targetType,

        @Positive(message = "대상 ID는 양수여야 합니다.")
        Long targetId,

        AdminActionType action,

        @Size(max = 200, message = "커서는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public AdminActionSearchRequest {
        size = size == null ? 20 : size;
    }
}
