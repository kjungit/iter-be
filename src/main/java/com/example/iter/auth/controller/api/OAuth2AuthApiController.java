package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.service.OAuth2AuthService;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.service.model.OAuthExchangeResult;
import com.example.iter.auth.support.RefreshTokenCookieManager;
import com.example.iter.auth.support.OAuth2ExchangeSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth2 Auth", description = "카카오 OAuth2 로그인·회원가입 API")
@RestController
@RequestMapping("/api/v1/auth/oauth2/kakao")
@RequiredArgsConstructor
public class OAuth2AuthApiController {

    private final OAuth2AuthService oAuth2AuthService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final OAuth2ExchangeSessionManager exchangeSessionManager;

    @Operation(
            summary = "카카오 로그인 교환",
            description = "OAuth2 임시 세션을 로그인 결과 또는 추가 절차 토큰으로 교환합니다. 요청 Body는 없습니다."
    )
    @Parameter(
            name = "X-XSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "XSRF-TOKEN Cookie와 동일한 CSRF Token"
    )
    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchange(HttpServletRequest request) {
        try {
            String exchangeCode = exchangeSessionManager.consume(request);
            OAuthExchangeResult result = oAuth2AuthService.exchange(exchangeCode);
            if (result instanceof OAuthExchangeResult.Authenticated authenticated) {
                return tokenResponse(authenticated.tokenPair(), HttpStatus.OK);
            }

            OAuthExchangeResult.ActionRequired actionRequired = (OAuthExchangeResult.ActionRequired) result;
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(actionRequired.response());
        } finally {
            exchangeSessionManager.invalidate(request);
        }
    }

    @Operation(
            summary = "카카오 신규 회원가입",
            description = "카카오 신규 회원의 추가 정보를 저장하고 즉시 로그인 처리합니다."
    )
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessTokenResponse> signUp(
            @Valid @RequestBody KakaoSignUpRequest request
    ) {
        IssuedTokenPair tokenPair = oAuth2AuthService.signUp(request);
        return tokenResponse(tokenPair, HttpStatus.CREATED);
    }

    private ResponseEntity<AccessTokenResponse> tokenResponse(
            IssuedTokenPair tokenPair,
            HttpStatus status
    ) {
        return ResponseEntity.status(status)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieManager.create(tokenPair.refreshToken()).toString()
                )
                .body(new AccessTokenResponse(tokenPair.accessToken()));
    }
}
