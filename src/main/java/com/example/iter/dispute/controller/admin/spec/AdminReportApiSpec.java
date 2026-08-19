package com.example.iter.dispute.controller.admin.spec;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.dispute.dto.request.AdminReportUpdateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Report", description = "관리자 일반 신고 조회 및 처리 API")
@SecurityRequirement(name = "JWT")
public interface AdminReportApiSpec {

    @Operation(
            summary = "신고 목록 조회",
            description = "관리자가 전체 신고를 대상 유형과 처리 상태 조건으로 최신 접수순 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 페이징 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<PageResponse<ReportSummaryResponse>> getReports(
            @Valid @ParameterObject ReportSearchRequest request
    );

    @Operation(
            summary = "신고 상세 조회",
            description = "관리자가 신고 내용, 신고자, 현재 처리 상태, 관리자 메모와 처리 완료 시각을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "신고 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "신고 또는 신고자 없음", content = @Content)
    })
    ResponseEntity<AdminReportDetailResponse> getReport(
            @Parameter(description = "조회할 신고 ID", example = "1", required = true)
            @Positive(message = "신고 ID는 1 이상이어야 합니다.") Long reportId
    );

    @Operation(
            summary = "신고 상태 변경",
            description = "신고를 검토 중·처리 완료·기각 상태로 변경하고 관리자 메모와 처리 이력을 기록합니다. 종료된 신고는 다시 변경할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 또는 신고 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "신고 또는 신고자 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 신고 상태 전이", content = @Content)
    })
    ResponseEntity<AdminReportDetailResponse> updateReportStatus(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "상태를 변경할 신고 ID", example = "1", required = true)
            @Positive(message = "신고 ID는 1 이상이어야 합니다.") Long reportId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 신고 상태와 관리자 처리 메모",
                    required = true
            )
            @Valid AdminReportUpdateRequest request
    );
}
