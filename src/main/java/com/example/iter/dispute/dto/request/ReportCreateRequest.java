package com.example.iter.dispute.dto.request;

import com.example.iter.dispute.domain.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상 유형은 필수입니다.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상 ID는 필수입니다.")
        @Positive(message = "신고 대상 ID는 양수여야 합니다.")
        Long targetId,

        @NotBlank(message = "신고 사유는 필수입니다.")
        @Size(max = 50, message = "신고 사유는 50자 이하여야 합니다.")
        String reason,

        @NotBlank(message = "신고 내용은 필수입니다.")
        @Size(max = 2000, message = "신고 내용은 2000자 이하여야 합니다.")
        String description
) {
}
