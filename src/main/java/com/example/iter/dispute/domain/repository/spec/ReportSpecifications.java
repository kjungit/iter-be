package com.example.iter.dispute.domain.repository.spec;

import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import org.springframework.data.jpa.domain.Specification;

// 로그인 사용자 본인의 신고 목록(searchMyReports) 조회 조건을 조립한다.
// value/countQuery를 손으로 두 벌 유지하지 않기 위해 Specification으로 작성.
public class ReportSpecifications {

    private ReportSpecifications() {
    }

    public static Specification<Report> reporterIs(Long reporterId) {
        return (root, query, cb) -> cb.equal(root.get("reporterId"), reporterId);
    }

    public static Specification<Report> hasTargetType(ReportTargetType targetType) {
        return (root, query, cb) ->
                targetType == null ? null : cb.equal(root.get("targetType"), targetType);
    }

    public static Specification<Report> hasStatus(ReportStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Report> myReports(Long reporterId, ReportTargetType targetType, ReportStatus status) {
        return Specification.where(reporterIs(reporterId))
                .and(hasTargetType(targetType))
                .and(hasStatus(status));
    }
}
