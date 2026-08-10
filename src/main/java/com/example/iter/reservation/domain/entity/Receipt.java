package com.example.iter.reservation.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ERD RECEIPT 엔티티 — 수령 시점 상태 기록 (Rental과 같은 도메인이므로 직접 연관관계 사용)
@Entity
@Table(name = "receipt")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Receipt extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 20)
    private ProductConditionType productCondition;

    @Column(name = "condition_detail", columnDefinition = "TEXT")
    private String conditionDetail;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
