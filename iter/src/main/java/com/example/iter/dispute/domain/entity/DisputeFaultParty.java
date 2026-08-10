package com.example.iter.dispute.domain.entity;

// ERD DISPUTE.fault_party
// [주의] ERD.md에 컬럼은 정의되어 있지만 실제 값 목록은 문서에 없어, 통상적인 분쟁 처리 케이스를 바탕으로 임시 정의함.
// 담당자(권상록)가 관리자 분쟁 처리 로직을 설계하며 팀 컨벤션에 맞게 확정/수정 필요.
public enum DisputeFaultParty {
    RENTER,  // 대여자 과실
    OWNER,   // 등록자 과실
    BOTH,    // 쌍방 과실
    NONE     // 과실 없음 (기각 등)
}
