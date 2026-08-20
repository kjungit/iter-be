package com.example.iter.payment.service;

import com.example.iter.payment.client.TossPaymentClient;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.dto.toss.TossConfirmApiResponse;
import com.example.iter.payment.dto.toss.TossWebhookData;
import com.example.iter.payment.dto.toss.TossWebhookPayload;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TossWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private TossWebhookService tossWebhookService;

    private Payment pendingPayment(String orderId) {
        Payment payment = Payment.builder().rentalId(10L).amount(BigDecimal.valueOf(100)).build();
        payment.assignOrder(orderId, BigDecimal.valueOf(100));
        return payment;
    }

    @Test
    void PAYMENT_STATUS_CHANGED가_아니면_아무것도_안한다() {
        var payload = new TossWebhookPayload("BILLING_DELETED", null);

        tossWebhookService.handle(payload);

        verify(tossPaymentClient, never()).getPayment(any());
    }

    @Test
    void 검증된_상태가_DONE이면_PENDING_결제를_PAID로_바꾸고_대여상태도_바꾼다() {
        Payment payment = pendingPayment("order-1");
        Rental rental = Rental.builder().id(10L).status(RentalStatus.PENDING).build();
        var payload = new TossWebhookPayload("PAYMENT_STATUS_CHANGED",
                new TossWebhookData("payKey", "order-1", "DONE"));

        when(tossPaymentClient.getPayment("payKey"))
                .thenReturn(new TossConfirmApiResponse("payKey", "order-1", "DONE", "2026-08-18T14:18:34+09:00", 100L));
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental));

        tossWebhookService.handle(payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaymentKey()).isEqualTo("payKey");
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    }

    @Test
    void 이미_PAID된_결제면_다시_반영하지_않는다() {
        Payment payment = pendingPayment("order-1");
        payment.markPaid("payKey", java.time.LocalDateTime.now());
        var payload = new TossWebhookPayload("PAYMENT_STATUS_CHANGED",
                new TossWebhookData("payKey", "order-1", "DONE"));

        when(tossPaymentClient.getPayment("payKey"))
                .thenReturn(new TossConfirmApiResponse("payKey", "order-1", "DONE", "2026-08-18T14:18:34+09:00", 100L));
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        tossWebhookService.handle(payload);

        verify(rentalRepository, never()).findById(any());
    }

    @Test
    void 매칭되는_결제가_없으면_예외없이_무시한다() {
        var payload = new TossWebhookPayload("PAYMENT_STATUS_CHANGED",
                new TossWebhookData("payKey", "order-unknown", "DONE"));

        when(tossPaymentClient.getPayment("payKey"))
                .thenReturn(new TossConfirmApiResponse("payKey", "order-unknown", "DONE", "2026-08-18T14:18:34+09:00", 100L));
        when(paymentRepository.findByOrderId("order-unknown")).thenReturn(Optional.empty());

        tossWebhookService.handle(payload);

        verify(rentalRepository, never()).findById(any());
    }

    @Test
    void 재조회한_상태가_DONE이_아니면_아직_반영하지_않는다() {
        Payment payment = pendingPayment("order-1");
        var payload = new TossWebhookPayload("PAYMENT_STATUS_CHANGED",
                new TossWebhookData("payKey", "order-1", "CANCELED"));

        when(tossPaymentClient.getPayment("payKey"))
                .thenReturn(new TossConfirmApiResponse("payKey", "order-1", "CANCELED", null, 100L));
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        tossWebhookService.handle(payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
