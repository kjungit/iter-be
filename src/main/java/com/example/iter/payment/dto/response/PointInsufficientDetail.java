package com.example.iter.payment.dto.response;

import java.math.BigDecimal;

// POINT_INSUFFICIENT(409) 에러 응답에 message 외에 같이 내려주는 부가 정보.
public record PointInsufficientDetail(BigDecimal pointBalance, BigDecimal requiredAmount) {
}
