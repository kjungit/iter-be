package com.example.iter.dispute.dto.request;

import com.example.iter.dispute.domain.entity.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReportUpdateRequest(
        @NotNull(message = "변경할 신고 상태는 필수입니다.")
        ReportStatus status,

        @NotBlank(message = "관리자 처리 메모는 필수입니다.")
        @Size(max = 2000, message = "관리자 처리 메모는 2000자 이하여야 합니다.")
        String adminMemo
) {
}
