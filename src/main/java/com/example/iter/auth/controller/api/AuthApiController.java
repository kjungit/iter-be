package com.example.iter.auth.controller.api;

import com.example.iter.auth.controller.api.spec.AuthApiSpec;
import com.example.iter.auth.domain.entity.PreferredLanguage;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Locale;
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
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController implements AuthApiSpec {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @Override
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request, Locale locale) {
        UserResponse response = authService.signUp(request, PreferredLanguage.fromLocale(locale));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokenPair tokenPair = authService.login(request);
        return withRefreshTokenCookie(tokenPair);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = refreshTokenCookieManager.extract(request)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        IssuedTokenPair tokenPair = authService.refresh(rawRefreshToken);
        return withRefreshTokenCookie(tokenPair);
    }

    @Override
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

    @Override
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AccessTokenResponse> withRefreshTokenCookie(IssuedTokenPair tokenPair) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken()).toString())
                .body(new AccessTokenResponse(tokenPair.accessToken()));
    }
}
