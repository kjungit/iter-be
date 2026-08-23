package com.example.iter.payment.service.model;

import com.example.iter.payment.domain.entity.PaymentStatus;

/**
 * 받은 대여 요청 목록에서 사용할 거래별 결제 상태 조회 결과입니다.
 */
public record RentalPaymentStatusRow(
        Long rentalId,
        PaymentStatus paymentStatus
) {
}
