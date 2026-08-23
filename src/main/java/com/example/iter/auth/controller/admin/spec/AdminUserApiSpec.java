package com.example.iter.auth.controller.admin.spec;

import com.example.iter.auth.dto.request.AdminUserSearchRequest;
import com.example.iter.auth.dto.request.AdminUserStatusRequest;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import com.example.iter.auth.dto.response.AdminUserStatusResponse;
import com.example.iter.auth.dto.response.AdminUserSummaryResponse;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
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

@Tag(name = "Admin User", description = "관리자 회원 조회 및 상태 관리 API")
@SecurityRequirement(name = "JWT")
public interface AdminUserApiSpec {

    @Operation(
            summary = "회원 목록 조회",
            description = "이메일·이름·닉네임 검색어와 회원 상태 조건으로 전체 회원을 최신 가입순으로 커서 조회합니다. 첫 요청에서는 cursor를 생략하고 다음 요청에는 응답의 nextCursor를 전달합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 커서 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<CursorPageResponse<AdminUserSummaryResponse>> getUsers(
            @Valid @ParameterObject AdminUserSearchRequest request
    );

    @Operation(
            summary = "회원 상세 조회",
            description = "특정 회원의 기본 정보와 빌린 거래·빌려준 거래·연체·피신고 건수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "회원 ID 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content)
    })
    ResponseEntity<AdminUserDetailResponse> getUser(
            @Parameter(description = "조회할 회원 ID", example = "1", required = true)
            @Positive(message = "회원 ID는 1 이상이어야 합니다.")
            Long userId
    );

    @Operation(
            summary = "회원 상태 변경",
            description = "일반 회원을 정지하거나 정지를 해제하고 관리자 처리 이력을 기록합니다. 관리자 계정 정지는 허용하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 형식 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 또는 관리자 계정 정지 시도", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 회원 상태 전이", content = @Content)
    })
    ResponseEntity<AdminUserStatusResponse> updateStatus(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "상태를 변경할 회원 ID", example = "1", required = true)
            @Positive(message = "회원 ID는 1 이상이어야 합니다.") Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 회원 상태와 관리자 처리 사유",
                    required = true
            )
            @Valid AdminUserStatusRequest request
    );
}
