package com.example.iter.auth.controller.admin;

import com.example.iter.auth.dto.request.AdminUserSearchRequest;
import com.example.iter.auth.dto.request.AdminUserStatusRequest;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import com.example.iter.auth.dto.response.AdminUserStatusResponse;
import com.example.iter.auth.dto.response.AdminUserSummaryResponse;
import com.example.iter.auth.service.AdminUserService;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin User", description = "관리자 회원 관리 API")
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "JWT")
public class AdminUserApiController {

    private final AdminUserService adminUserService;

    // 관리자 회원 목록을 조회합니다
    @Operation(summary = "회원 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserSummaryResponse>> getUsers(@Valid @ModelAttribute AdminUserSearchRequest request) {
        return ResponseEntity.ok(adminUserService.getUsers(request));
    }


    // 특정 회원의 정보와 거래 요약을 조회합니다.
    @Operation(summary = "회원 상세 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUser(
            @PathVariable
            @Positive(message = "회원 ID는 1 이상이어야 합니다.")
            Long userId
    ) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    // 회원 상태를 정지 또는 정지 해제로 변경합니다.
    @Operation(summary = "회원 상태 변경")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserStatusResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,

            @PathVariable
            @Positive(message = "회원 ID는 1 이상이어야 합니다.")
            Long userId,

            @Valid @RequestBody AdminUserStatusRequest request
    ) {
        Long adminId = principal.getUser().getId();

        return ResponseEntity.ok(
                adminUserService.updateStatus(
                        adminId,
                        userId,
                        request
                )
        );
    }

}
