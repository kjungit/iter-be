package com.example.iter.dispute.controller.api.spec;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.dispute.dto.request.ReportCreateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
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

@Tag(name = "Report", description = "회원·장비·거래 일반 신고 API")
@SecurityRequirement(name = "JWT")
public interface ReportApiSpec {

    @Operation(
            summary = "신고 접수",
            description = "회원, 장비 또는 거래를 신고합니다. 거래 신고는 대여자 또는 등록자만 가능하며 처리 중인 동일 대상 신고는 중복 접수할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신고 접수 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 오류 또는 본인 소유 대상 신고", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "정지·탈퇴 회원 또는 거래 제3자", content = @Content),
            @ApiResponse(responseCode = "404", description = "신고 대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "처리 중인 동일 대상 신고 존재", content = @Content)
    })
    ResponseEntity<ReportDetailResponse> createReport(
            @Parameter(hidden = true) CustomUserDetails principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "신고 대상 유형·ID와 신고 사유·내용",
                    required = true
            )
            @Valid ReportCreateRequest request
    );

    @Operation(
            summary = "내 신고 목록 조회",
            description = "로그인 사용자가 작성한 신고만 대상 유형과 처리 상태 조건으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 신고 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 페이징 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content)
    })
    ResponseEntity<PageResponse<ReportSummaryResponse>> getMyReports(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Valid @ParameterObject ReportSearchRequest request
    );

    @Operation(
            summary = "내 신고 상세 조회",
            description = "로그인 사용자가 직접 작성한 신고의 상세 내용을 조회합니다. 타인의 신고는 존재 여부를 노출하지 않고 조회할 수 없는 신고로 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 신고 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "신고 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 조회할 수 없는 신고", content = @Content)
    })
    ResponseEntity<ReportDetailResponse> getMyReport(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "조회할 신고 ID", example = "1", required = true)
            @Positive(message = "신고 ID는 1 이상이어야 합니다.") Long reportId
    );
}
