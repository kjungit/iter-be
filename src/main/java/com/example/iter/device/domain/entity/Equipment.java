package com.example.iter.device.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// ERD EQUIPMENT 엔티티
// ownerId는 auth 도메인 User의 PK를 값으로만 참조한다 (도메인 간 JPA 연관관계를 걸지 않음 — AdminAction과 동일한 이유).
@Entity
@Table(name = "equipment")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Equipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "daily_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal dailyPrice;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_to")
    private LocalDate availableTo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "product_condition", nullable = false, length = 20)
    private ProductConditionType productCondition = ProductConditionType.NORMAL;

    @Column(name = "condition_detail", columnDefinition = "TEXT")
    private String conditionDetail;

    // ===== 도메인 메서드 =====

    public boolean isOwnedBy(Long userId) {
        return this.ownerId.equals(userId);
    }

    public void changeStatus(EquipmentStatus status) {
        this.status = status;
    }
}
