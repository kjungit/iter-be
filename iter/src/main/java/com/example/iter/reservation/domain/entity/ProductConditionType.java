package com.example.iter.reservation.domain.entity;

// ERD RECEIPT/RETURN_RECEIPT.product_condition — device 도메인의 동명 enum과 값 집합은 같지만
// 도메인 결합을 피하기 위해 독립적으로 정의한다.
public enum ProductConditionType {
    NORMAL,
    DAMAGED,
    DIRTY,
    MISSING_PART,
    OTHER
}
