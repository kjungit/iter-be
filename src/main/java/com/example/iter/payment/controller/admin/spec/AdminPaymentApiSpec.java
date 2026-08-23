package com.example.iter.payment.controller.admin.spec;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.payment.dto.request.AdminPaymentSearchRequest;
import com.example.iter.payment.dto.response.AdminPaymentDetailResponse;
import com.example.iter.payment.dto.response.AdminPaymentSummaryResponse;
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

@Tag(name = "Admin Payment", description = "관리자 결제 기록 조회 API")
@SecurityRequirement(name = "JWT")
public interface AdminPaymentApiSpec {

    @Operation(
            summary = "결제 목록 조회",
            description = "주문번호·장비명·대여자 정보, 결제 상태와 생성일 범위로 결제 기록을 최신순 커서 조회합니다. 첫 요청에서는 cursor를 생략하고 다음 요청에는 응답의 nextCursor를 전달합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 커서 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<CursorPageResponse<AdminPaymentSummaryResponse>> getPayments(
            @Valid @ParameterObject AdminPaymentSearchRequest request
    );

    @Operation(
            summary = "결제 상세 조회",
            description = "특정 결제의 상태, 대여자와 예약 당시 장비·대여 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "결제 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "결제 기록 없음", content = @Content)
    })
    ResponseEntity<AdminPaymentDetailResponse> getPayment(
            @Parameter(description = "조회할 결제 ID", example = "1", required = true)
            @Positive(message = "결제 ID는 1 이상이어야 합니다.") Long paymentId
    );
}
