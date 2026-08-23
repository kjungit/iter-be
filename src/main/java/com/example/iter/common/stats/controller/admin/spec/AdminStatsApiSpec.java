package com.example.iter.common.stats.controller.admin.spec;

import com.example.iter.common.stats.dto.response.AdminStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Stats", description = "관리자 대시보드 통계 API")
@SecurityRequirement(name = "JWT")
public interface AdminStatsApiSpec {

    @Operation(
            summary = "관리자 대시보드 통계 조회",
            description = "전체 회원 수, 등록 장비 수, 접수된 신고 수, 전체 결제 건수를 각각 COUNT 쿼리로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통계 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content)
    })
    ResponseEntity<AdminStatsResponse> getStats();
}
