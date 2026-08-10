package com.example.iter.dispute.domain.entity;

public enum ReportStatus {
    RECEIVED,      // 신고 접수
    UNDER_REVIEW,  // 관리자 검토 중
    RESOLVED,      // 조치 완료
    REJECTED       // 기각
}
