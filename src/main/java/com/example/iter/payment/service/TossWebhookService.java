package com.example.iter.payment.service;

import com.example.iter.payment.client.TossPaymentClient;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.toss.TossConfirmApiResponse;
import com.example.iter.payment.dto.toss.TossWebhookPayload;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 토스 웹훅(POST /api/v1/webhooks/toss)이 실제로 하는 일.
// 웹훅 바디에 있는 값(orderId/status 등)은 위조될 수 있으므로 절대 그대로 믿지 않는다 —
// paymentKey로 토스에 재조회(getPayment)해서 받은 값만 신뢰한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class TossWebhookService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public void handle(TossWebhookPayload payload) {
        if (!payload.isPaymentStatusChanged()) {
            log.info("토스 웹훅 처리 제외: 지원하지 않는 이벤트 유형");
            return;
        }

        TossConfirmApiResponse verified = tossPaymentClient.getPayment(payload.data().paymentKey());

        Payment payment = paymentRepository.findByOrderId(verified.orderId()).orElse(null);
        if (payment == null) {
            log.warn("토스 웹훅 처리 실패: 매칭되는 결제 내역 없음");
            return;
        }

        if (!"DONE".equals(verified.status())) {
            log.info("토스 웹훅 처리 제외: paymentId={}, rentalId={}, providerStatus={}",
                    payment.getId(), payment.getRentalId(), verified.status());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("토스 웹훅 중복 처리 방지: paymentId={}, rentalId={}, status={}",
                    payment.getId(), payment.getRentalId(), payment.getStatus());
            return;
        }

        // confirm() 응답보다 웹훅이 먼저 도착하는 경우를 대비한 방어적 반영
        payment.markPaid(verified.paymentKey(), verified.approvedAtAsLocalDateTime());
        rentalRepository.findById(payment.getRentalId())
                .ifPresent(rental -> rental.changeStatus(RentalStatus.REQUESTED));
        log.info("토스 웹훅 결제 상태 반영 처리: paymentId={}, rentalId={}, status={}",
                payment.getId(), payment.getRentalId(), payment.getStatus());
    }
}
