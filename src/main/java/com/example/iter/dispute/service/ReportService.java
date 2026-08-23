package com.example.iter.dispute.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.dispute.domain.repository.spec.ReportSpecifications;
import com.example.iter.dispute.dto.request.ReportCreateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.util.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<ReportStatus> ACTIVE_REPORT_STATUSES = EnumSet.of(
            ReportStatus.RECEIVED,
            ReportStatus.UNDER_REVIEW
    );

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportTargetValidator reportTargetValidator;
    private final ReportMapper reportMapper;

    // 신고 대상과 처리 중인 중복 신고를 검증한 뒤 신고를 접수합니다.
    @Transactional
    public ReportDetailResponse createReport(Long reporterId, ReportCreateRequest request) {
        User reporter = getActiveReporterWithLock(reporterId);

        reportTargetValidator.validate(
                request.targetType(),
                request.targetId(),
                reporterId
        );

        validateDuplicateActiveReport(reporterId, request);

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason().trim())
                .description(request.description().trim())
                .status(ReportStatus.RECEIVED)
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("신고 접수 처리: reportId={}, reporterId={}, targetType={}, targetId={}, status={}",
                savedReport.getId(), reporterId, savedReport.getTargetType(),
                savedReport.getTargetId(), savedReport.getStatus());

        return reportMapper.toDetail(savedReport, reporter);
    }

    // 로그인 사용자가 작성한 신고를 검색 조건과 페이지 정보로 조회합니다.
    @Transactional(readOnly = true)
    public PageResponse<ReportSummaryResponse> getMyReports(Long reporterId, ReportSearchRequest request) {
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<ReportSummaryResponse> reports = reportRepository.findAll(
                ReportSpecifications.myReports(reporterId, request.targetType(), request.status()),
                reportPageable(request.page(), request.size())
        ).map(report -> reportMapper.toSummary(report, reporter));

        return PageResponse.from(reports);
    }

    // 로그인 사용자가 작성한 특정 신고의 상세 정보를 조회합니다.
    @Transactional(readOnly = true)
    public ReportDetailResponse getMyReport(Long reporterId, Long reportId) {
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Report report = reportRepository.findByIdAndReporterId(reportId, reporterId).orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        return reportMapper.toDetail(report, reporter);
    }

    // 신고 생성 동시 요청을 직렬화하고 신고자가 현재 이용 가능한 상태인지 검증합니다.
    private User getActiveReporterWithLock(Long reporterId) {
        User reporter = userRepository.findWithLockById(reporterId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (reporter.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        if (reporter.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }

        return reporter;
    }

    // 같은 사용자의 같은 대상 신고가 이미 처리 중이면 신고 생성을 제한합니다.
    private void validateDuplicateActiveReport(Long reporterId, ReportCreateRequest request) {
        boolean duplicateExists = reportRepository.existsActiveReport(
                reporterId,
                request.targetType(),
                request.targetId(),
                ACTIVE_REPORT_STATUSES
        );

        if (duplicateExists) {
            throw new CustomException(ErrorCode.DUPLICATE_ACTIVE_REPORT);
        }
    }

    // 신고 목록을 최신 접수 순으로 조회할 페이지 정보를 생성합니다.
    private Pageable reportPageable(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }
}
