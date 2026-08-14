package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.dto.response.OAuthActionRequiredResponse;
import com.example.iter.auth.service.OAuth2AuthService;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.service.model.OAuthExchangeResult;
import com.example.iter.auth.support.OAuth2ExchangeSessionManager;
import com.example.iter.auth.support.RefreshTokenCookieManager;
import com.example.iter.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카카오 계정 로그인 성공",
                    content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "회원가입 또는 기존 계정 연결 필요",
                    content = @Content(schema = @Schema(implementation = OAuthActionRequiredResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "OAuth 임시 세션 또는 일회용 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF Token이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/exchange")
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
    @Parameter(
            name = "X-XSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "XSRF-TOKEN Cookie와 동일한 CSRF Token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "카카오 신규 회원가입 및 로그인 성공",
                    content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 또는 카카오 이메일 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "OAuth 일회용 토큰이 유효하지 않거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF Token이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이메일 또는 카카오 계정이 이미 사용 중",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
