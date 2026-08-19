package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.request.SignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.auth.service.AuthService;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.support.RefreshTokenCookieManager;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        UserResponse response = authService.signUp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "로그인", description = "Access Token은 응답 본문으로, Refresh Token은 HttpOnly Cookie로 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokenPair tokenPair = authService.login(request);
        return withRefreshTokenCookie(tokenPair);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token Cookie를 회전하고 새로운 Access Token을 응답 본문으로 발급합니다.")
    @Parameters({
            @Parameter(name = "__Secure-iter-refresh", in = ParameterIn.COOKIE, required = true,
                    description = "로그인 또는 이전 재발급에서 받은 HttpOnly Refresh Token Cookie"),
            @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
                    description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = refreshTokenCookieManager.extract(request)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        IssuedTokenPair tokenPair = authService.refresh(rawRefreshToken);
        return withRefreshTokenCookie(tokenPair);
    }

    @Operation(summary = "로그아웃", description = "현재 세션의 Refresh Token을 폐기하고 Cookie를 삭제합니다.",
            security = @SecurityRequirement(name = "JWT"))
    @Parameters({
            @Parameter(name = "__Secure-iter-refresh", in = ParameterIn.COOKIE,
                    description = "폐기할 HttpOnly Refresh Token Cookie. 누락되어도 로그아웃은 멱등 처리됩니다."),
            @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
                    description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest request
    ) {
        refreshTokenCookieManager.extract(request)
                .ifPresent(token -> authService.logout(principal.getUser().getId(), token));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.delete().toString())
                .build();
    }

    @Operation(summary = "CSRF Token 발급", description = "Refresh Token Cookie를 사용하는 인증 요청용 CSRF Token을 발급합니다.")
    @ApiResponse(responseCode = "204", description = "CSRF Token Cookie 발급 완료")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AccessTokenResponse> withRefreshTokenCookie(IssuedTokenPair tokenPair) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken()).toString())
                .body(new AccessTokenResponse(tokenPair.accessToken()));
    }
}
