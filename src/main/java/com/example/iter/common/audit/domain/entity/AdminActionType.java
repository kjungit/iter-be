package com.example.iter.common.audit.domain.entity;

// ERD에 명시된 값 + 팀에서 필요한 액션 추가 예정 (예: 분쟁 처리 관련 액션)
public enum AdminActionType {
    SUSPEND_USER,       // 회원 이용 정지
    RESTORE_USER,       // 회원 이용 정지 해제

    SUSPEND_EQUIPMENT,  // 장비 차단
    RESTORE_EQUIPMENT,   // 장비 차단 해제

    RESOLVE_REPORT,     // 일반 신고 처리 완료
    REJECT_REPORT,      // 일반 신고 기각
    REVIEW_REPORT,      // 신고 검토 중

    RESOLVE_DISPUTE     // 분쟁 처리 완료
}
