package com.example.iter.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingRegisterRequest(
        @NotBlank(message = "택배사는 필수입니다.")
        @Size(max = 50, message = "택배사는 50자 이하여야 합니다.")
        String carrier,

        @NotBlank(message = "운송장 번호는 필수입니다.")
        @Size(max = 50, message = "운송장 번호는 50자 이하여야 합니다.")
        String trackingNumber
) {
}
