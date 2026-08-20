package com.example.iter.common.audit.controller.admin.spec;

import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Action", description = "관리자 처리 이력 조회 API")
@SecurityRequirement(name = "JWT")
public interface AdminActionApiSpec {

    @Operation(
            summary = "관리자 처리 이력 조회",
            description = "대상 유형, 대상 ID, 관리자 조치 유형 조건으로 감사 로그를 최신 처리순 조회합니다. 전달하지 않은 조건은 적용하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관리자 처리 이력 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 또는 페이징 조건 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<PageResponse<AdminActionResponse>> getAdminActions(
            @Valid @ParameterObject AdminActionSearchRequest request
    );
}
