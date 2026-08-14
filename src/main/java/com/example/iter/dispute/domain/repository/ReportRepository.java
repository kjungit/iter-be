package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 특정 유형과 대상 ID로 접수된 신고 수를 조회합니다.
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}
