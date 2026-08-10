package com.example.iter.device.domain.entity;

// ERD EQUIPMENT.product_condition (RECEIPT/RETURN_RECEIPT 등 reservation 도메인에도 동일한 값 집합이 쓰이지만,
// 도메인 간 결합을 피하기 위해 각 도메인에서 독립적으로 정의한다)
public enum ProductConditionType {
    NORMAL,        // 정상
    DAMAGED,       // 파손
    DIRTY,         // 오염
    MISSING_PART,  // 구성품 누락
    OTHER          // 기타 이상
}
