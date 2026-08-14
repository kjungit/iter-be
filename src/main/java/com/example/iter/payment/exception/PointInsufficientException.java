package com.example.iter.payment.exception;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PointInsufficientException extends RuntimeException {

    private final BigDecimal pointBalance;
    private final BigDecimal requiredAmount;

    public PointInsufficientException(BigDecimal pointBalance, BigDecimal requiredAmount) {
        super("포인트가 부족합니다.");
        this.pointBalance = pointBalance;
        this.requiredAmount = requiredAmount;
    }
}
