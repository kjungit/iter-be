package com.example.iter.dispute.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// REPORT 엔티티 — 대여 건과 무관하게 회원/장비 자체를 신고하는 "일반 신고" (rental 단위 분쟁인 DISPUTE와는 구분됨)
// reporterId/targetId는 auth(User) 또는 device(Equipment) 도메인 PK를 값으로만 참조 (도메인 간 결합 최소화)
//
// [주의] 구체적인 필드 목록은 ERD 변경사항에 target_type/status enum만 명시되어 있어
// DISPUTE 엔티티와 동일한 형태로 유추해 작성했습니다. 실제 컬럼 구성은 팀 확인 후 조정해주세요.
@Entity
@Table(name = "report")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;


    // 관리자가 신고 상태와 처리 메모를 변경합니다. 해결 또는 기각 상태에서는 처리 완료 시각을 저장합니다.
    public void changeStatusByAdmin(
            ReportStatus status,
            String adminMemo,
            LocalDateTime processedAt
    ) {
        this.status = status;
        this.adminMemo = adminMemo;

        if (status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED) {
            this.resolvedAt = processedAt;
            return;
        }

        this.resolvedAt = null;
    }
}
