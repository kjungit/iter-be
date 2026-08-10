package com.example.iter.delivery.domain.entity;

// ERD SHIPPING.type
public enum ShippingType {
    OUTBOUND,  // 장비 등록자 -> 대여자
    RETURN     // 대여자 -> 장비 등록자
}
