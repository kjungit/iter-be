package com.example.iter.payment.dto.response;

import com.example.iter.common.exception.ErrorCode;

import java.math.BigDecimal;

// POINT_INSUFFICIENT(409) 에러 응답.
// 공통 ErrorResponse{code,message}로는 pointBalance/requiredAmount 같이 실을 수 없어서 같은 모양(code, message)에 필드를 추가한 전용 타입으로 분리
public record PointInsufficientErrorResponse(
        String code,
        String message,
        BigDecimal pointBalance,
        BigDecimal requiredAmount
) {
    public static PointInsufficientErrorResponse of(String message, BigDecimal pointBalance, BigDecimal requiredAmount) {
        return new PointInsufficientErrorResponse(ErrorCode.POINT_INSUFFICIENT.name(), message, pointBalance, requiredAmount);
    }
}
