package com.example.iter.dispute.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.service.AdminActionService;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.dispute.dto.request.AdminReportSearchRequest;
import com.example.iter.dispute.dto.request.AdminReportUpdateRequest;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.util.AdminReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AdminActionService adminActionService;
    private final AdminReportMapper adminReportMapper;

    // 관리자가 대상 유형과 상태 조건으로 전체 신고 목록을 커서 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<ReportSummaryResponse> getReports(AdminReportSearchRequest request) {
        CursorKey cursorKey = CursorCodec.decode(request.cursor());
        List<Report> reports = reportRepository.searchForAdminByCursor(
                request.targetType(),
                request.status(),
                cursorKey == null ? null : cursorKey.createdAt(),
                cursorKey == null ? null : cursorKey.id(),
                PageRequest.of(0, request.size() + 1)
        );

        Map<Long, User> reporterMap = loadReporters(reports);

        return CursorPageResponse.from(
                reports,
                request.size(),
                report -> adminReportMapper.toSummary(
                        report,
                        getRequiredReporter(reporterMap, report.getReporterId())
                ),
                report -> new CursorKey(report.getCreatedAt(), report.getId())
        );
    }

    // 관리자가 특정 신고의 상세 정보와 관리자 처리 정보를 조회합니다.
    @Transactional(readOnly = true)
    public AdminReportDetailResponse getReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        User reporter = userRepository.findById(report.getReporterId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return adminReportMapper.toDetail(report, reporter);
    }

    // 관리자가 신고 상태와 처리 메모를 변경하고 관리자 조치 이력을 저장합니다.
    @Transactional
    public AdminReportDetailResponse updateReportStatus(
            Long adminId,
            Long reportId,
            AdminReportUpdateRequest request
    ) {
        Report report = reportRepository.findWithLockById(reportId).orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        validateStatusChange(report.getStatus(), request.status());
        AdminActionType action = toAdminActionType(request.status());

        report.changeStatusByAdmin(
                request.status(),
                request.adminMemo().trim(),
                LocalDateTime.now()
        );

        adminActionService.record(
                adminId,
                AdminActionTargetType.REPORT,
                report.getId(),
                action,
                request.adminMemo().trim()
        );

        reportRepository.flush();
        log.info("관리자 신고 상태 변경 처리: adminId={}, reportId={}, action={}, status={}",
                adminId, reportId, action, report.getStatus());

        User reporter = userRepository.findById(report.getReporterId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return adminReportMapper.toDetail(report, reporter);
    }

    // 현재 신고 상태에서 요청 상태로 변경할 수 있는지 검증합니다.
    private void validateStatusChange(
            ReportStatus currentStatus,
            ReportStatus requestedStatus
    ) {
        // 접수된 신고만 관리자 검토 중 상태로 변경할 수 있습니다.
        boolean canStartReview = currentStatus == ReportStatus.RECEIVED
                && requestedStatus == ReportStatus.UNDER_REVIEW;

        // 접수된 신고를 별도의 검토 중 단계를 거치지 않고 바로 처리 완료 또는 기각 상태로 변경할 수 있습니다.
        boolean canCloseFromReceived = currentStatus == ReportStatus.RECEIVED
                && (requestedStatus == ReportStatus.RESOLVED
                || requestedStatus == ReportStatus.REJECTED);

        // 관리자 검토 중인 신고를 처리 완료 또는 기각 상태로 변경할 수 있습니다.
        boolean canCloseFromReview = currentStatus == ReportStatus.UNDER_REVIEW
                && (requestedStatus == ReportStatus.RESOLVED
                || requestedStatus == ReportStatus.REJECTED);

        if (!canStartReview && !canCloseFromReceived && !canCloseFromReview) {
            throw new CustomException(
                    ErrorCode.INVALID_REPORT_STATUS_TRANSITION
            );
        }
    }

    // 변경할 신고 상태를 관리자 조치 이력 유형으로 변환합니다.
    private AdminActionType toAdminActionType(ReportStatus requestedStatus) {
        return switch (requestedStatus) {
            case UNDER_REVIEW -> AdminActionType.REVIEW_REPORT;
            case RESOLVED -> AdminActionType.RESOLVE_REPORT;
            case REJECTED -> AdminActionType.REJECT_REPORT;
            case RECEIVED -> throw new CustomException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION);
        };
    }

    // 한 페이지에 포함된 신고자를 한 번에 조회해 회원 ID 기준 Map으로 변환합니다.
    private Map<Long, User> loadReporters(List<Report> reports) {
        if (reports.isEmpty()) {
            return Map.of();
        }

        List<Long> reporterIds = reports.stream().map(Report::getReporterId).distinct().toList();

        return userRepository.findAllById(reporterIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    // 신고자 Map에서 회원을 찾고 데이터가 없으면 예외를 발생시킵니다.
    private User getRequiredReporter(Map<Long, User> reporterMap, Long reporterId) {
        User reporter = reporterMap.get(reporterId);

        if (reporter == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return reporter;
    }
}
