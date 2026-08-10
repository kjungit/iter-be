package com.example.iter.dispute.domain.entity;

// ERD DISPUTE.status
public enum DisputeStatus {
    REPORTED,       // 분쟁 접수
    INVESTIGATING,  // 관리자 검토 중
    RESOLVED,       // 분쟁 해결
    REJECTED        // 분쟁 사유가 인정되지 않아 기각
}
