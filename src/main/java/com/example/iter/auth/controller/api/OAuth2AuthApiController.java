package com.example.iter.auth.controller.api;

import com.example.iter.auth.controller.api.spec.OAuth2AuthApiSpec;
import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.response.AccessTokenResponse;
import com.example.iter.auth.service.OAuth2AuthService;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.service.model.OAuthExchangeResult;
import com.example.iter.auth.support.OAuth2ExchangeSessionManager;
import com.example.iter.auth.support.RefreshTokenCookieManager;
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

@RestController
@RequestMapping("/api/v1/auth/oauth2/kakao")
@RequiredArgsConstructor
public class OAuth2AuthApiController implements OAuth2AuthApiSpec {

    private final OAuth2AuthService oAuth2AuthService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final OAuth2ExchangeSessionManager exchangeSessionManager;

    @Override
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

    @Override
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
