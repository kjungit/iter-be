package com.example.iter.delivery.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ERD SHIPPING 엔티티 (mock 배송 — 기획서 4-1 #7, #10 참고)
// rentalId는 reservation 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화)
// 하나의 Rental에 출고(OUTBOUND)/반송(RETURN) 두 건이 생길 수 있어 rentalId는 unique가 아님
@Entity
@Table(name = "shipping")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Shipping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingType type;

    @Column(length = 50)
    private String carrier;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ShippingStatus status = ShippingStatus.READY;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // ===== 도메인 메서드 =====

    public void markDelivered(LocalDateTime deliveredAt) {
        this.status = ShippingStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }
}
