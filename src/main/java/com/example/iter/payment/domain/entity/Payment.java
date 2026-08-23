package com.example.iter.payment.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ERD PAYMENT 엔티티 (mock 결제 — 기획서 4-1 #6, 9-1 참고)
// rentalId는 reservation 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화)
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(
                        name = "idx_payment_created_id",
                        columnList = "created_at DESC, id DESC"
                ),
                @Index(
                        name = "idx_payment_status_created_id",
                        columnList = "status, created_at DESC, id DESC"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false, unique = true)
    private Long rentalId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "order_id", unique = true)
    private String orderId;

    @Column(name = "payment_key")
    private String paymentKey;

    // 토스 Idempotency-Key 헤더용. confirm은 orderId 발급 시점에 고정, cancel은 최초 취소 시도 시점에 고정 —
    // 같은 결제건에 대한 재시도가 항상 같은 키를 써야 토스가 중복 요청을 첫 응답 그대로 돌려준다.
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "cancel_idempotency_key")
    private String cancelIdempotencyKey;

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    public void assignOrder(String orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    // cancel은 PAID 상태에서만 일어나므로 최초 호출 시점에 한 번만 발급하고, 이후 재시도는 같은 값을 재사용한다.
    public String ensureCancelIdempotencyKey() {
        if (this.cancelIdempotencyKey == null) {
            this.cancelIdempotencyKey = UUID.randomUUID().toString();
        }
        return this.cancelIdempotencyKey;
    }

    public void markPaid(String paymentKey, LocalDateTime approveAt) {
        this.paymentKey = paymentKey;
        this.status =PaymentStatus.PAID;
        this.paidAt = approveAt;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}
