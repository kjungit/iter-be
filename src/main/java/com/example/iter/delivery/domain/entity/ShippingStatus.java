package com.example.iter.delivery.domain.entity;

// ERD SHIPPING.status
public enum ShippingStatus {
    READY,       // 배송 준비
    SHIPPED,     // 발송 완료
    IN_TRANSIT,  // 배송 중
    DELIVERED    // 배송 완료
}
