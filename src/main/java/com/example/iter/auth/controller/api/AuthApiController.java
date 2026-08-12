package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.request.SignUpRequest;
import com.example.iter.auth.dto.response.TokenResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// MVP 기능 #1 회원가입/로그인(JWT)
@Tag(name = "Auth", description = "회원가입/로그인 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        UserResponse response = authService.signUp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "로그인", description = "성공 시 access token / refresh token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // TODO: POST /refresh (refresh token으로 access token 재발급), POST /logout
}
