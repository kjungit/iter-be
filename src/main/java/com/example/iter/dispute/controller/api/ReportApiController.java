package com.example.iter.dispute.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.dispute.controller.api.spec.ReportApiSpec;
import com.example.iter.dispute.dto.request.ReportCreateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportApiController implements ReportApiSpec {

    private final ReportService reportService;

    // 회원, 장비 또는 거래를 신고합니다.
    @Override
    @PostMapping
    public ResponseEntity<ReportDetailResponse> createReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ReportCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 작성한 신고 목록을 조회합니다.
    @Override
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ReportSummaryResponse>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute ReportSearchRequest request
    ) {
        return ResponseEntity.ok(reportService.getMyReports(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 작성한 특정 신고의 상세 정보를 조회합니다.
    @Override
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponse> getMyReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(reportService.getMyReport(principal.getUser().getId(), reportId));
    }
}
