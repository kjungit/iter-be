package com.example.iter.auth.controller.admin;

import com.example.iter.auth.controller.admin.spec.AdminUserApiSpec;
import com.example.iter.auth.dto.request.AdminUserSearchRequest;
import com.example.iter.auth.dto.request.AdminUserStatusRequest;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import com.example.iter.auth.dto.response.AdminUserStatusResponse;
import com.example.iter.auth.dto.response.AdminUserSummaryResponse;
import com.example.iter.auth.service.AdminUserService;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController implements AdminUserApiSpec {

    private final AdminUserService adminUserService;

    // 관리자 회원 목록을 조회합니다
    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<AdminUserSummaryResponse>> getUsers(@ModelAttribute AdminUserSearchRequest request) {
        return ResponseEntity.ok(adminUserService.getUsers(request));
    }


    // 특정 회원의 정보와 거래 요약을 조회합니다.
    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    // 회원 상태를 정지 또는 정지 해제로 변경합니다.
    @Override
    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserStatusResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,

            @PathVariable Long userId,

            @RequestBody AdminUserStatusRequest request
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
