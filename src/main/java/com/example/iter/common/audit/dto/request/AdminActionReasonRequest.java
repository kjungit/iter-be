package com.example.iter.common.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminActionReasonRequest(
        @NotBlank(message = "관리자 처리 사유는 필수입니다.")
        @Size(max = 500, message = "관리자 처리 사유는 500자 이하여야 합니다.")
        String reason
) {
}
