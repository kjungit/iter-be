package com.example.iter.notification.domain.entity;

// 알림 종류. requiresEmail은 이 타입의 알림이 생성될 때 메일도 같이 보낼지를 결정
// (기획 확정: 결제완료/대여신청/취소 > 장비 등록자(owner)에게 메일, 승인/거절 > 대여자(renter)에게 메일.
//  같은 결제 확인 시점에 발생하는 결제완료/대여신청도 각각 별도 메일로 발송)
public enum NotificationType {

    PAYMENT_COMPLETED_OWNER(true),   // 결제 완료 - 장비 등록자에게: 대금 수령 안내
    PAYMENT_COMPLETED_RENTER(false), // 결제 완료 - 대여자에게: 본인 결제 확인용 (메일 없이 앱 알림만)
    RENTAL_REQUESTED(true),          // 새 대여 신청 도착 - 장비 등록자에게
    RENTAL_APPROVED(true),           // 대여 승인 - 대여자에게
    RENTAL_REJECTED(true),           // 대여 거절 - 대여자에게
    RENTAL_CANCELED(true),           // 승인 전 예약 취소 - 장비 등록자에게
    RENTAL_RECEIVED(true),           // 수령확인 완료(대여 시작) - 장비 등록자에게
    REVIEW_RECEIVED(false);          // 상대방이 리뷰를 남김 - 리뷰 대상자에게 (메일 없이 앱 알림만)

    private final boolean requiresEmail;

    NotificationType(boolean requiresEmail) {
        this.requiresEmail = requiresEmail;
    }

    public boolean requiresEmail() {
        return requiresEmail;
    }
}
