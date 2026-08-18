package com.example.iter.dispute.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.dispute.dto.request.ReportCreateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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

@Tag(name = "Report", description = "일반 신고 API")
@Validated
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class ReportApiController {

    private final ReportService reportService;

    // 회원, 장비 또는 거래를 신고합니다.
    @Operation(summary = "신고 접수")
    @PostMapping
    public ResponseEntity<ReportDetailResponse> createReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 작성한 신고 목록을 조회합니다.
    @Operation(summary = "내 신고 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ReportSummaryResponse>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute ReportSearchRequest request
    ) {
        return ResponseEntity.ok(reportService.getMyReports(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 작성한 특정 신고의 상세 정보를 조회합니다.
    @Operation(summary = "내 신고 상세 조회")
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponse> getMyReport(
            @AuthenticationPrincipal CustomUserDetails principal,

            @PathVariable
            @Positive(message = "신고 ID는 1 이상이어야 합니다.")
            Long reportId
    ) {
        return ResponseEntity.ok(reportService.getMyReport(principal.getUser().getId(), reportId));
    }
}
