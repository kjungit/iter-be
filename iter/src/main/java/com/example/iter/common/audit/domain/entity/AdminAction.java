package com.example.iter.common.audit.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

// ERD ADMIN_ACTION — 관리자 조치 감사 로그.
// adminId/targetId는 다른 도메인(User/Equipment/Dispute)의 PK를 값으로만 들고,
// JPA 연관관계(FK 매핑)는 의도적으로 걸지 않는다 (도메인 간 결합도를 낮춰 3차 MSA 전환을 염두에 둔 설계 — 개발 컨벤션 참고).
@Entity
@Table(name = "admin_action")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdminAction extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AdminActionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminActionType action;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
