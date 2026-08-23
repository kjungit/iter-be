package com.example.iter.dispute.domain.repository;

import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    // 특정 유형과 대상 ID로 접수된 신고 수를 조회합니다.
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 로그인 사용자가 작성한 특정 신고를 조회합니다.
    Optional<Report> findByIdAndReporterId(Long reportId, Long reporterId);

    // searchMyReports는 ReportSpecifications로 조립해서 findAll(spec, pageable)로 호출한다
    // (value/countQuery를 손으로 두 벌 유지하지 않기 위해 Specification으로 전환).

    // 같은 사용자가 같은 대상을 현재 처리 중인 상태로 신고했는지 확인합니다.
    @Query("""
            select case when count(r.id) > 0 then true else false end
            from Report r
            where r.reporterId = :reporterId
              and r.targetType = :targetType
              and r.targetId = :targetId
              and r.status in :statuses
            """)
    boolean existsActiveReport(
            @Param("reporterId") Long reporterId,
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("statuses") Collection<ReportStatus> statuses
    );

    // 관리자가 전체 신고를 대상 유형과 처리 상태로 필터링해 조회합니다. 대상 유형이나 처리 상태가 null이면 해당 조건은 적용하지 않습니다.
    @Query(
            value = """
                select r
                from Report r
                where (:targetType is null or r.targetType = :targetType)
                  and (:status is null or r.status = :status)
                """,
            countQuery = """
                select count(r.id)
                from Report r
                where (:targetType is null or r.targetType = :targetType)
                  and (:status is null or r.status = :status)
                """
    )
    Page<Report> searchForAdmin(
            @Param("targetType") ReportTargetType targetType,
            @Param("status") ReportStatus status,
            Pageable pageable
    );

    // 동일 신고의 상태 변경이 동시에 처리되지 않도록 신고 행을 비관적 쓰기 락으로 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Report> findWithLockById(Long reportId);
}


