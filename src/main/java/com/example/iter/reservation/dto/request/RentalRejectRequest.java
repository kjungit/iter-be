package com.example.iter.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RentalRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다.")
        @Size(max = 200, message = "거절 사유는 200자 이하여야 합니다.")
        String reason
) {
}
