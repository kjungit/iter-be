package com.example.iter.payment.dto.response;

import java.time.LocalDateTime;

public record AdminPaymentDetailResponse(
        AdminPaymentSummaryResponse payment,
        AdminPaymentRentalResponse rental,
        LocalDateTime updatedAt
) {
}
