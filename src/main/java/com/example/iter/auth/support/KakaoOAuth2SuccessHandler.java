package com.example.iter.auth.support;

import com.example.iter.auth.config.OAuth2FlowProperties;
import com.example.iter.auth.service.OAuthPendingTokenService;
import com.example.iter.auth.service.model.KakaoUserInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class KakaoOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthPendingTokenService pendingTokenService;
    private final KakaoOAuth2UserInfoMapper userInfoMapper;
    private final OAuth2FlowProperties properties;
    private final KakaoOAuth2FailureHandler failureHandler;
    private final OAuth2ExchangeSessionManager exchangeSessionManager;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Token)
                    || !"kakao".equals(oAuth2Token.getAuthorizedClientRegistrationId())) {
                throw new IllegalArgumentException("지원하지 않는 OAuth2 인증 제공자입니다.");
            }

            KakaoUserInfo userInfo = userInfoMapper.map(oAuth2Token.getPrincipal());
            String exchangeCode = pendingTokenService.issueLoginExchange(userInfo);
            exchangeSessionManager.store(request, exchangeCode);
            String redirectUri = properties.getFrontendCallbackUri();

            SecurityContextHolder.clearContext();
            redirectStrategy.sendRedirect(request, response, redirectUri);
        } catch (Exception exception) {
            failureHandler.onAuthenticationFailure(
                    request,
                    response,
                    new OAuth2AuthenticationException(
                            new OAuth2Error("kakao_login_processing_failed"),
                            exception
                    )
            );
        }
    }

}
