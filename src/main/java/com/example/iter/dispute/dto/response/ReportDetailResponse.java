package com.example.iter.dispute.dto.response;

import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ReportDetailResponse(
        Long reportId,
        UserSummaryResponse reporter,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
