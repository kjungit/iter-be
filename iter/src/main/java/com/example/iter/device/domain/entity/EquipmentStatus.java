package com.example.iter.device.domain.entity;

// ERD EQUIPMENT.status
public enum EquipmentStatus {
    AVAILABLE,    // 정상적으로 대여 가능한 장비
    UNAVAILABLE,  // 등록자가 일시적으로 공개/대여 중지
    MAINTENANCE,  // 점검 또는 수리 중이라 대여 불가
    SUSPENDED,    // 관리자가 문제를 발견하여 차단
    DELETED       // 등록자가 삭제한 장비
}
