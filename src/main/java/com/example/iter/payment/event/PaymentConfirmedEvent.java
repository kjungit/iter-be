package com.example.iter.payment.event;

// 결제 confirm 성공 시점에 발행. notification 모듈이 구독해 owner(결제완료+대여신청)/renter(결제완료) 알림을 만든다.
// rentalId만 들고 다니고 나머지(수신자, 장비명, 금액 등)는 리스너가 커밋된 최신 상태를 다시 조회해서 채운다 —
// 이벤트에 스냅샷을 실어 보내면 리스너가 처리되는 시점과 실제 값이 어긋날 수 있기 때문.
public record PaymentConfirmedEvent(Long rentalId) {
}
