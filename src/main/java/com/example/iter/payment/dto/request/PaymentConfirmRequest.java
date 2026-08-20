package com.example.iter.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        @NotNull(message = "amount는 필수입니다.")
        @Positive(message = "amount는 0보다 커야 합니다.")
        BigDecimal amount
) {
}
