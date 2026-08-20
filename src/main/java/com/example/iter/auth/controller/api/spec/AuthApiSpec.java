package com.example.iter.auth.controller.api.spec;

import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.request.SignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.dto.response.UserResponse;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "Auth", description = "회원가입/로그인 API")
public interface AuthApiSpec {

    @Operation(summary = "회원가입")
    ResponseEntity<UserResponse> signUp(SignUpRequest request);

    @Operation(summary = "로그인", description = "Access Token은 응답 본문으로, Refresh Token은 HttpOnly Cookie로 발급합니다.")
    ResponseEntity<AccessTokenResponse> login(LoginRequest request);

    @Operation(summary = "토큰 재발급", description = "Refresh Token Cookie를 회전하고 새로운 Access Token을 응답 본문으로 발급합니다.")
    @Parameters({
            @Parameter(name = "__Secure-iter-refresh", in = ParameterIn.COOKIE, required = true,
                    description = "로그인 또는 이전 재발급에서 받은 HttpOnly Refresh Token Cookie"),
            @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
                    description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    })
    ResponseEntity<AccessTokenResponse> refresh(
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(summary = "로그아웃", description = "현재 세션의 Refresh Token을 폐기하고 Cookie를 삭제합니다.",
            security = @SecurityRequirement(name = "JWT"))
    @Parameters({
            @Parameter(name = "__Secure-iter-refresh", in = ParameterIn.COOKIE,
                    description = "폐기할 HttpOnly Refresh Token Cookie. 누락되어도 로그아웃은 멱등 처리됩니다."),
            @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
                    description = "XSRF-TOKEN Cookie와 동일한 CSRF Token")
    })
    ResponseEntity<Void> logout(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(summary = "CSRF Token 발급", description = "Refresh Token Cookie를 사용하는 인증 요청용 CSRF Token을 발급합니다.")
    @ApiResponse(responseCode = "204", description = "CSRF Token Cookie 발급 완료")
    ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken csrfToken);
}
