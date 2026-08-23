package com.example.iter.dispute.dto.request;

import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminReportSearchRequest(
        ReportTargetType targetType,

        ReportStatus status,

        @Size(max = 200, message = "커서는 200자 이하여야 합니다.")
        String cursor,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
    public AdminReportSearchRequest {
        size = size == null ? 20 : size;
    }
}
