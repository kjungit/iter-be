package com.example.iter.dispute.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.service.AdminActionService;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.dispute.dto.request.AdminReportSearchRequest;
import com.example.iter.dispute.dto.request.AdminReportUpdateRequest;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.util.AdminReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REPORTER_ID = 2L;
    private static final Long REPORT_ID = 10L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionService adminActionService;

    @Mock
    private AdminReportMapper adminReportMapper;

    @InjectMocks
    private AdminReportService adminReportService;

    @Test
    void 신고_목록을_조건과_최신순으로_조회하고_신고자를_일괄_조회한다() {
        Report first = report(10L, ReportStatus.RECEIVED);
        Report second = report(11L, ReportStatus.RECEIVED);
        User reporter = reporter();
        ReportSummaryResponse firstResponse = summary(10L);
        ReportSummaryResponse secondResponse = summary(11L);
        AdminReportSearchRequest request = new AdminReportSearchRequest(
                ReportTargetType.EQUIPMENT,
                ReportStatus.RECEIVED,
                null,
                2
        );

        when(reportRepository.searchForAdminByCursor(
                eq(ReportTargetType.EQUIPMENT),
                eq(ReportStatus.RECEIVED),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(userRepository.findAllById(List.of(REPORTER_ID))).thenReturn(List.of(reporter));
        when(adminReportMapper.toSummary(first, reporter)).thenReturn(firstResponse);
        when(adminReportMapper.toSummary(second, reporter)).thenReturn(secondResponse);

        CursorPageResponse<ReportSummaryResponse> result = adminReportService.getReports(request);

        assertThat(result.content()).containsExactly(firstResponse, secondResponse);
        verify(userRepository).findAllById(List.of(REPORTER_ID));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportRepository).searchForAdminByCursor(
                eq(ReportTargetType.EQUIPMENT),
                eq(ReportStatus.RECEIVED),
                eq(null),
                eq(null),
                captor.capture()
        );
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void 신고_목록이_비어있으면_신고자를_조회하지_않는다() {
        AdminReportSearchRequest request = new AdminReportSearchRequest(null, null, null, null);
        when(reportRepository.searchForAdminByCursor(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(List.of());

        CursorPageResponse<ReportSummaryResponse> result = adminReportService.getReports(request);

        assertThat(result.content()).isEmpty();
        verify(userRepository, never()).findAllById(any());
        verify(adminReportMapper, never()).toSummary(any(), any());
    }

    @Test
    void 신고_목록의_신고자를_찾을_수_없으면_예외가_발생한다() {
        Report report = report(REPORT_ID, ReportStatus.RECEIVED);
        AdminReportSearchRequest request = new AdminReportSearchRequest(null, null, null, 20);
        when(reportRepository.searchForAdminByCursor(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(List.of(report));
        when(userRepository.findAllById(List.of(REPORTER_ID))).thenReturn(List.of());

        assertThatThrownBy(() -> adminReportService.getReports(request))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
                );

        verify(adminReportMapper, never()).toSummary(any(), any());
    }

    @Test
    void 신고_상세를_조회한다() {
        Report report = report(REPORT_ID, ReportStatus.RECEIVED);
        User reporter = reporter();
        AdminReportDetailResponse expected = org.mockito.Mockito.mock(AdminReportDetailResponse.class);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(adminReportMapper.toDetail(report, reporter)).thenReturn(expected);

        assertThat(adminReportService.getReport(REPORT_ID)).isSameAs(expected);
    }

    @Test
    void 존재하지_않는_신고_상세는_조회할_수_없다() {
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReportService.getReport(REPORT_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND)
                );

        verify(userRepository, never()).findById(any());
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void 허용된_신고_상태로_변경하고_관리자_조치를_기록한다(
            ReportStatus currentStatus,
            ReportStatus requestedStatus,
            AdminActionType expectedAction
    ) {
        Report report = report(REPORT_ID, currentStatus);
        User reporter = reporter();
        AdminReportUpdateRequest request = new AdminReportUpdateRequest(requestedStatus, "  처리 메모  ");
        AdminReportDetailResponse expected = org.mockito.Mockito.mock(AdminReportDetailResponse.class);
        when(reportRepository.findWithLockById(REPORT_ID)).thenReturn(Optional.of(report));
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(adminReportMapper.toDetail(report, reporter)).thenReturn(expected);

        AdminReportDetailResponse result = adminReportService.updateReportStatus(
                ADMIN_ID,
                REPORT_ID,
                request
        );

        assertThat(result).isSameAs(expected);
        assertThat(report.getStatus()).isEqualTo(requestedStatus);
        assertThat(report.getAdminMemo()).isEqualTo("처리 메모");
        if (requestedStatus == ReportStatus.RESOLVED || requestedStatus == ReportStatus.REJECTED) {
            assertThat(report.getResolvedAt()).isNotNull();
        } else {
            assertThat(report.getResolvedAt()).isNull();
        }
        verify(adminActionService).record(
                ADMIN_ID,
                AdminActionTargetType.REPORT,
                REPORT_ID,
                expectedAction,
                "처리 메모"
        );
        verify(reportRepository).flush();
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void 허용되지_않는_신고_상태_변경은_거절한다(
            ReportStatus currentStatus,
            ReportStatus requestedStatus
    ) {
        Report report = report(REPORT_ID, currentStatus);
        when(reportRepository.findWithLockById(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.updateReportStatus(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportUpdateRequest(requestedStatus, "처리 메모")
        ))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REPORT_STATUS_TRANSITION)
                );

        assertThat(report.getStatus()).isEqualTo(currentStatus);
        verify(adminActionService, never()).record(any(), any(), any(), any(), any());
        verify(reportRepository, never()).flush();
    }

    @Test
    void 상태를_변경할_신고가_없으면_예외가_발생한다() {
        when(reportRepository.findWithLockById(REPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReportService.updateReportStatus(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportUpdateRequest(ReportStatus.RESOLVED, "처리 메모")
        ))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND)
                );

        verify(adminActionService, never()).record(any(), any(), any(), any(), any());
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.UNDER_REVIEW, AdminActionType.REVIEW_REPORT),
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.RESOLVED, AdminActionType.RESOLVE_REPORT),
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.REJECTED, AdminActionType.REJECT_REPORT),
                Arguments.of(ReportStatus.UNDER_REVIEW, ReportStatus.RESOLVED, AdminActionType.RESOLVE_REPORT),
                Arguments.of(ReportStatus.UNDER_REVIEW, ReportStatus.REJECTED, AdminActionType.REJECT_REPORT)
        );
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.RECEIVED),
                Arguments.of(ReportStatus.UNDER_REVIEW, ReportStatus.RECEIVED),
                Arguments.of(ReportStatus.RESOLVED, ReportStatus.UNDER_REVIEW),
                Arguments.of(ReportStatus.RESOLVED, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.REJECTED, ReportStatus.RESOLVED)
        );
    }

    private Report report(Long id, ReportStatus status) {
        return Report.builder()
                .id(id)
                .reporterId(REPORTER_ID)
                .targetType(ReportTargetType.EQUIPMENT)
                .targetId(100L)
                .reason("신고 사유")
                .description("신고 내용")
                .status(status)
                .build();
    }

    private User reporter() {
        return User.builder()
                .id(REPORTER_ID)
                .email("reporter@iter.test")
                .password("encoded-password")
                .name("신고자")
                .nickname("신고자")
                .build();
    }

    private ReportSummaryResponse summary(Long reportId) {
        return new ReportSummaryResponse(
                reportId,
                null,
                ReportTargetType.EQUIPMENT,
                100L,
                "신고 사유",
                ReportStatus.RECEIVED,
                null
        );
    }
}
