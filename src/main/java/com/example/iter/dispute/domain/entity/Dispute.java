package com.example.iter.dispute.domain.entity;

import com.example.iter.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ERD DISPUTE 엔티티 (기획서 4-1 #13, 6-2 "분쟁 자동 감지" 참고)
// rentalId/reporterId/respondentId는 각각 reservation/auth 도메인 PK를 값으로만 참조
@Entity
@Table(name = "dispute")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Dispute extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "respondent_id", nullable = false)
    private Long respondentId;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private DisputeStatus status = DisputeStatus.REPORTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "fault_party", length = 10)
    private DisputeFaultParty faultParty;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
