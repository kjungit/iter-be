package com.example.iter.payment.dto.response;

import com.example.iter.reservation.domain.entity.Rental;

import java.math.BigDecimal;

public record PaymentReadyResponse(
        Long rentalId,
        String orderId,
        String orderName,
        BigDecimal amount,
        String clientKey,
        String customerKey
) {
    public static PaymentReadyResponse of( Rental rental, String orderId, BigDecimal amount, String clientKey ) {
        return new PaymentReadyResponse(
                rental.getId(),
                orderId,
                rental.getProductNameSnapshot(),
                amount,
                clientKey,
                "user-" + rental.getRenterId()
        );
    }

}
