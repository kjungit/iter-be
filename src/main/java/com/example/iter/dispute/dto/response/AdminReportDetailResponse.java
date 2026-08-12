package com.example.iter.dispute.dto.response;

import java.time.LocalDateTime;

public record AdminReportDetailResponse(
        ReportDetailResponse report,
        String adminMemo,
        LocalDateTime updatedAt
) {
}
