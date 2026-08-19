package com.example.iter.dispute.controller.admin;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.dispute.controller.admin.spec.AdminReportApiSpec;
import com.example.iter.dispute.dto.request.AdminReportUpdateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportApiController implements AdminReportApiSpec {

    private final AdminReportService adminReportService;

    // 관리자가 대상 유형과 처리 상태 조건으로 전체 신고 목록을 조회합니다.
    @Override
    @GetMapping
    public ResponseEntity<PageResponse<ReportSummaryResponse>> getReports(@ModelAttribute ReportSearchRequest request) {
        return ResponseEntity.ok(adminReportService.getReports(request));
    }

    // 관리자가 특정 신고의 신고 내용과 처리 정보를 조회합니다.
    @Override
    @GetMapping("/{reportId}")
    public ResponseEntity<AdminReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminReportService.getReport(reportId));
    }

    // 관리자가 신고 상태와 처리 메모를 변경하고 관리자 조치 이력을 저장합니다.
    @Override
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<AdminReportDetailResponse> updateReportStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long reportId,
            @RequestBody AdminReportUpdateRequest request
    ) {
        Long adminId = principal.getUser().getId();

        return ResponseEntity.ok(adminReportService.updateReportStatus(adminId, reportId, request));
    }
}
