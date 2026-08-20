package com.example.iter.payment.service.model;

import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.reservation.domain.entity.Rental;

public record PaymentHistoryRow(
        Payment payment,
        Rental rental
) {
}
