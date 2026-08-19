package com.example.iter.auth.support;

import com.example.iter.auth.config.OAuth2FlowProperties;
import com.example.iter.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuth2FailureHandler implements AuthenticationFailureHandler {

    private final OAuth2FlowProperties properties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.warn("카카오 OAuth2 인증 실패: {}", exception.getClass().getSimpleName());
        String redirectUri = UriComponentsBuilder
                .fromUriString(properties.getFrontendCallbackUri())
                .queryParam("error", ErrorCode.OAUTH_AUTHENTICATION_FAILED.name())
                .build()
                .encode()
                .toUriString();
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        redirectStrategy.sendRedirect(request, response, redirectUri);
    }
}
