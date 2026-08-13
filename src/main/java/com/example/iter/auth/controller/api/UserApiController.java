package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 마이페이지 API
// 지금은 JWT 인증 체인이 실제로 동작하는지 확인용 "내 정보 조회"만 구현
// TODO: 내 정보 수정, 비밀번호 변경, 회원 탈퇴, 기본 배송지 조회/수정
@Tag(name = "User", description = "마이페이지 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    @Operation(summary = "내 정보 조회", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "JWT"))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }
}
