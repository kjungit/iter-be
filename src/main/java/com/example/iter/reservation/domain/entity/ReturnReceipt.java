package com.example.iter.reservation.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// ERD RETURN_RECEIPT 엔티티 — 반납 시점 상태 기록. Receipt(수령 시점)와 비교해 분쟁 자동 감지에 사용됨 (기획서 6-2 참고)
@Entity
@Table(name = "return_receipt")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReturnReceipt extends BaseCreatedAtEntity {

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

    @Column(name = "return_date")
    private LocalDate returnDate;
}
