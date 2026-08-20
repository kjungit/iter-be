package com.example.iter.reservation.controller.api.spec;

import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
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

@Tag(name = "Return", description = "반납 증빙 비교 및 최종 확인 API")
@SecurityRequirement(name = "JWT")
public interface ReturnApiSpec {

    @Operation(
            summary = "반납 확인 대상 목록 조회",
            description = "로그인 사용자가 등록자인 거래 중 반송이 도착해 최종 확인이 필요한 RETURNED 거래를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 확인 대상 조회 성공"),
            @ApiResponse(responseCode = "400", description = "페이징 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "사용자 권한 없음", content = @Content)
    })
    ResponseEntity<PageResponse<ReturnTargetResponse>> getReturnTargets(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Valid @ParameterObject PagingRequest request
    );

    @Operation(
            summary = "수령·반납 증빙 비교",
            description = "거래 당사자가 수령 당시와 반납 당시의 상품 상태, 설명, 사진 및 기록 시각을 비교 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 증빙 비교 조회 성공"),
            @ApiResponse(responseCode = "400", description = "대여 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "거래 당사자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "거래·장비·회원 또는 증빙 없음", content = @Content)
    })
    ResponseEntity<ReturnComparisonResponse> getReturnComparison(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "비교할 대여 거래 ID", example = "101", required = true)
            @Positive(message = "대여 ID는 1 이상이어야 합니다.") Long rentalId
    );

    @Operation(
            summary = "반납 최종 확인",
            description = "장비 등록자가 반납을 정상 완료하거나, 이상 반납을 분쟁으로 접수합니다. 정상은 COMPLETED, 이상은 DISPUTED로 전환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 최종 확인 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 또는 대여 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "장비 등록자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "거래·장비 또는 증빙 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 확인했거나 최종 확인할 수 없는 거래 상태", content = @Content)
    })
    ResponseEntity<ReturnConfirmationResponse> confirmReturn(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "최종 확인할 대여 거래 ID", example = "101", required = true)
            @Positive(message = "대여 ID는 1 이상이어야 합니다.") Long rentalId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "상품 이상 여부와 이상 반납 시 분쟁 사유·설명",
                    required = true
            )
            @Valid ReturnConfirmationRequest request
    );
}
