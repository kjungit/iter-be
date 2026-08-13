package com.example.iter.dispute.dto.request;

import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReportSearchRequest(
        ReportTargetType targetType,

        ReportStatus status,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public ReportSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
