package com.example.iter.reservation.domain.entity;

// ERD RENTAL.status — 대여 생명주기 전체를 나타내는 상태 (기획서 6-2 상태 전이 참고)
// PENDING -> REQUESTED -> APPROVED -> SHIPPING -> RECEIVED -> RENTING
// -> RETURN_REQUESTED -> RETURNING -> RETURNED -> COMPLETED
// (REJECTED/CANCELED/DISPUTED는 분기 상태)
public enum RentalStatus {
    PENDING,           // 결제 대기
    REQUESTED,         // 결제 완료 후 장비 등록자의 승인 대기
    APPROVED,          // 장비 등록자가 대여 승인
    REJECTED,          // 장비 등록자가 대여 요청 거절
    CANCELED,          // 대여자가 승인 전에 취소
    SHIPPING,          // 장비를 대여자에게 배송 중
    RECEIVED,          // 대여자가 상품 수령 확인
    RENTING,           // 실제 대여 중
    RETURN_REQUESTED,  // 대여자가 반납 신청
    RETURNING,         // 장비를 등록자에게 반송 중
    RETURNED,          // 등록자가 장비 반납 도착 확인
    DISPUTED,          // 반납 과정에서 이상이 발생하여 분쟁 중
    COMPLETED          // 거래 최종 완료
}
