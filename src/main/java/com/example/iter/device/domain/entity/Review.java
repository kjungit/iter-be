package com.example.iter.device.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

// 리뷰 기능은 현재 비활성화되어 애플리케이션 조회에서 사용하지 않습니다.
// 기존 DB 스키마 검증과 향후 기능 복구 가능성을 위해 엔티티 매핑만 유지합니다.
// rentalId, userId는 각각 reservation/auth 도메인의 PK를 값으로만 참조합니다.
@Entity
@Table(name = "review")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false, unique = true)
    private Long rentalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String content;
}
