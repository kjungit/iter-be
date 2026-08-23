package com.example.iter.device.controller.admin.spec;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.dto.request.AdminEquipmentSearchRequest;
import com.example.iter.device.dto.request.AdminEquipmentStatusRequest;
import com.example.iter.device.dto.response.AdminEquipmentDetailResponse;
import com.example.iter.device.dto.response.AdminEquipmentSummaryResponse;
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

@Tag(name = "Admin Equipment", description = "관리자 장비 조회 및 차단 관리 API")
@SecurityRequirement(name = "JWT")
public interface AdminEquipmentApiSpec {

    @Operation(
            summary = "장비 목록 조회",
            description = "장비명·카테고리·장비 상태 조건으로 삭제된 장비를 포함한 전체 장비를 최신 등록순으로 커서 조회합니다. 첫 요청에서는 cursor를 생략하고 다음 요청에는 응답의 nextCursor를 전달합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장비 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 커서 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<CursorPageResponse<AdminEquipmentSummaryResponse>> getEquipments(
            @Valid @ParameterObject AdminEquipmentSearchRequest request
    );

    @Operation(
            summary = "장비 상세 조회",
            description = "특정 장비의 등록자, 가격, 대여 가능 기간, 상태, 상품 상태 및 전체 이미지를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장비 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "장비 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "장비 또는 등록자 없음", content = @Content)
    })
    ResponseEntity<AdminEquipmentDetailResponse> getEquipmentDetail(
            @Parameter(description = "조회할 장비 ID", example = "1", required = true)
            @Positive(message = "장비 ID는 1 이상이어야 합니다.")
            Long equipmentId
    );

    @Operation(
            summary = "장비 상태 변경",
            description = "ACTIVE 또는 INACTIVE 장비를 SUSPENDED로 차단합니다. 차단 해제 시에는 장비를 INACTIVE로 복구하며, 다시 활성화할지는 등록자가 결정합니다. 관리자 처리 이력도 함께 기록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장비 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "장비 또는 등록자 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 장비 상태 전이", content = @Content)
    })
    ResponseEntity<AdminEquipmentDetailResponse> updateEquipmentStatus(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "상태를 변경할 장비 ID", example = "1", required = true)
            @Positive(message = "장비 ID는 1 이상이어야 합니다.") Long equipmentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 장비 상태(SUSPENDED: 차단, INACTIVE: 차단 해제)와 관리자 처리 사유",
                    required = true
            )
            @Valid AdminEquipmentStatusRequest request
    );
}
