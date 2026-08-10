package com.example.iter.reservation.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// ERD RENTAL 엔티티
// equipmentId/renterId는 각각 device/auth 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화).
// *_snapshot 필드들은 예약 시점의 장비 정보를 그대로 복사해두는 값 — 이후 장비 정보가 바뀌어도 과거 예약 내역이 변하지 않도록 함.
//
// [주의] ERD.md의 RENTAL 테이블 정의에는 version 컬럼이 빠져 있지만,
// 기획서 5-1/8-1(Day5)에 명시된 "JPA 낙관적 락(@Version)" 동시성 제어를 구현하려면 반드시 필요한 컬럼이라 추가해두었다.
// -> ERD 원본(drawSQL) 업데이트가 필요하니 팀 공유 바랍니다.
@Entity
@Table(name = "rental")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Rental extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "renter_id", nullable = false)
    private Long renterId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "product_name_snapshot", nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(name = "category_snapshot", length = 50)
    private String categorySnapshot;

    @Column(name = "daily_price_snapshot", nullable = false)
    private BigDecimal dailyPriceSnapshot;

    @Column(name = "rental_days", nullable = false)
    private int rentalDays;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "receiver_name", length = 20)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(length = 10)
    private String zipcode;

    @Column(length = 200)
    private String address;

    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private RentalStatus status = RentalStatus.PENDING;

    // ===== 도메인 메서드 =====

    public boolean isRenter(Long userId) {
        return this.renterId.equals(userId);
    }

    public void changeStatus(RentalStatus status) {
        this.status = status;
    }
}
