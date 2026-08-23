package com.example.iter.notification.service;

import com.example.iter.auth.domain.entity.PreferredLanguage;

import java.util.Map;

// NotificationEventListener가 만드는 알림의 제목/본문/보간 파라미터를 한곳에 모아둔다.
// 한국어 문구/보간 동작은 이전(각 이벤트 핸들러에 흩어져 있던 리터럴)과 완전히 동일하게 유지했다 — 순수 이동.
//
// title/message는 이메일 발송에만 쓰인다 (그 자리에서 조립해서 바로 보내고 저장하지 않음).
// 이메일은 발사 후 소멸(fire-and-forget)이라 보내는 시점에 언어가 확정돼야 하므로, 수신자의
// User.preferredLanguage를 보고 한국어/영어 중 하나를 그 자리에서 고른다.
//
// params는 Notification 엔티티에 저장돼서 API로 그대로 나가고, 프론트가 type + params로
// 자체 i18n 사전을 통해 화면 표시 문구(제목/본문)를 조립한다 — 이메일과 무관한 별도 경로다.
// (자세한 내용은 docs/i18n-frontend-handoff.md 참고)
final class NotificationMessages {

    private static final String PAYMENT_COMPLETED_OWNER_TITLE_KO = "결제가 완료되었습니다";
    private static final String PAYMENT_COMPLETED_OWNER_TEMPLATE_KO = "%s님이 [%s] 대여 건의 결제를 완료했습니다.";
    private static final String PAYMENT_COMPLETED_OWNER_TITLE_EN = "Payment completed";
    private static final String PAYMENT_COMPLETED_OWNER_TEMPLATE_EN = "%s completed payment for your [%s] rental.";

    private static final String RENTAL_REQUESTED_TITLE_KO = "새로운 대여 신청이 도착했습니다";
    private static final String RENTAL_REQUESTED_TEMPLATE_KO =
            "[%s] 대여 신청이 도착했습니다. 승인 대기 목록을 확인해주세요.";
    private static final String RENTAL_REQUESTED_TITLE_EN = "New rental request received";
    private static final String RENTAL_REQUESTED_TEMPLATE_EN =
            "A rental request for [%s] has arrived. Please check your pending approvals.";

    private static final String PAYMENT_COMPLETED_RENTER_TITLE_KO = "결제가 완료되었습니다";
    private static final String PAYMENT_COMPLETED_RENTER_TEMPLATE_KO =
            "[%s] 대여 결제가 완료되었습니다. 등록자의 승인을 기다려주세요.";
    private static final String PAYMENT_COMPLETED_RENTER_TITLE_EN = "Payment completed";
    private static final String PAYMENT_COMPLETED_RENTER_TEMPLATE_EN =
            "Your payment for [%s] is complete. Please wait for the owner's approval.";

    private static final String RENTAL_APPROVED_TITLE_KO = "대여 요청이 승인되었습니다";
    private static final String RENTAL_APPROVED_TEMPLATE_KO = "[%s] 대여 요청이 승인되었습니다.";
    private static final String RENTAL_APPROVED_TITLE_EN = "Rental request approved";
    private static final String RENTAL_APPROVED_TEMPLATE_EN = "Your rental request for [%s] has been approved.";

    private static final String RENTAL_REJECTED_TITLE_KO = "대여 요청이 거절되었습니다";
    private static final String RENTAL_REJECTED_TEMPLATE_KO = "[%s] 대여 요청이 거절되었습니다. 사유: %s.%s";
    private static final String RENTAL_REJECTED_REFUND_NOTICE_KO = " 결제 금액은 환불 처리되었습니다.";
    private static final String RENTAL_REJECTED_TITLE_EN = "Rental request rejected";
    private static final String RENTAL_REJECTED_TEMPLATE_EN = "Your rental request for [%s] was rejected. Reason: %s.%s";
    private static final String RENTAL_REJECTED_REFUND_NOTICE_EN = " Your payment has been refunded.";

    private static final String RENTAL_CANCELED_TITLE_KO = "대여 요청이 취소되었습니다";
    private static final String RENTAL_CANCELED_TEMPLATE_KO = "%s님이 [%s] 대여 요청을 취소했습니다.";
    private static final String RENTAL_CANCELED_TITLE_EN = "Rental request canceled";
    private static final String RENTAL_CANCELED_TEMPLATE_EN = "%s canceled the rental request for [%s].";

    private static final String RENTAL_RECEIVED_TITLE_KO = "대여자가 수령을 확인했습니다";
    private static final String RENTAL_RECEIVED_TEMPLATE_KO =
            "%s님이 [%s] 물품 수령을 확인했습니다. 대여가 시작됩니다.";
    private static final String RENTAL_RECEIVED_TITLE_EN = "Renter confirmed receipt";
    private static final String RENTAL_RECEIVED_TEMPLATE_EN =
            "%s confirmed receipt of [%s]. The rental period has started.";

    private NotificationMessages() {
    }

    record Content(String title, String message, Map<String, Object> params) {
    }

    private static boolean isEnglish(PreferredLanguage language) {
        return language == PreferredLanguage.EN;
    }

    static Content paymentCompletedOwner(String renterName, String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? PAYMENT_COMPLETED_OWNER_TITLE_EN : PAYMENT_COMPLETED_OWNER_TITLE_KO,
                (en ? PAYMENT_COMPLETED_OWNER_TEMPLATE_EN : PAYMENT_COMPLETED_OWNER_TEMPLATE_KO)
                        .formatted(renterName, productName),
                Map.of("renterName", renterName, "productName", productName)
        );
    }

    static Content rentalRequested(String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? RENTAL_REQUESTED_TITLE_EN : RENTAL_REQUESTED_TITLE_KO,
                (en ? RENTAL_REQUESTED_TEMPLATE_EN : RENTAL_REQUESTED_TEMPLATE_KO).formatted(productName),
                Map.of("productName", productName)
        );
    }

    static Content paymentCompletedRenter(String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? PAYMENT_COMPLETED_RENTER_TITLE_EN : PAYMENT_COMPLETED_RENTER_TITLE_KO,
                (en ? PAYMENT_COMPLETED_RENTER_TEMPLATE_EN : PAYMENT_COMPLETED_RENTER_TEMPLATE_KO).formatted(productName),
                Map.of("productName", productName)
        );
    }

    static Content rentalApproved(String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? RENTAL_APPROVED_TITLE_EN : RENTAL_APPROVED_TITLE_KO,
                (en ? RENTAL_APPROVED_TEMPLATE_EN : RENTAL_APPROVED_TEMPLATE_KO).formatted(productName),
                Map.of("productName", productName)
        );
    }

    static Content rentalRejected(String productName, String rejectReason, boolean refunded, PreferredLanguage language) {
        boolean en = isEnglish(language);
        String refundNotice = refunded
                ? (en ? RENTAL_REJECTED_REFUND_NOTICE_EN : RENTAL_REJECTED_REFUND_NOTICE_KO)
                : "";
        return new Content(
                en ? RENTAL_REJECTED_TITLE_EN : RENTAL_REJECTED_TITLE_KO,
                (en ? RENTAL_REJECTED_TEMPLATE_EN : RENTAL_REJECTED_TEMPLATE_KO)
                        .formatted(productName, rejectReason, refundNotice),
                Map.of("productName", productName, "rejectReason", rejectReason, "refunded", refunded)
        );
    }

    static Content rentalCanceled(String renterName, String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? RENTAL_CANCELED_TITLE_EN : RENTAL_CANCELED_TITLE_KO,
                (en ? RENTAL_CANCELED_TEMPLATE_EN : RENTAL_CANCELED_TEMPLATE_KO).formatted(renterName, productName),
                Map.of("renterName", renterName, "productName", productName)
        );
    }

    static Content rentalReceived(String renterName, String productName, PreferredLanguage language) {
        boolean en = isEnglish(language);
        return new Content(
                en ? RENTAL_RECEIVED_TITLE_EN : RENTAL_RECEIVED_TITLE_KO,
                (en ? RENTAL_RECEIVED_TEMPLATE_EN : RENTAL_RECEIVED_TEMPLATE_KO).formatted(renterName, productName),
                Map.of("renterName", renterName, "productName", productName)
        );
    }
}
