package com.example.iter.common.security;

import com.example.iter.auth.domain.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 모든 요청마다 한 번 실행되어, Authorization 헤더의 JWT로부터 인증 상태를 복원하는 필터.
// 1) Authorization: Bearer <token> 헤더에서 토큰 추출
// 2) 서명/만료 검증
// 3) 유효하면 토큰의 사용자 id로 DB에서 최신 User를 조회해 SecurityContext에 등록
// 4) 토큰이 없거나 무효하면 인증 없이 통과 -> 보호된 경로는 체인 끝에서 401로 거부됨
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            TokenStatus status = jwtTokenProvider.validateToken(token);

            if (status == TokenStatus.VALID) {
                Long userId = jwtTokenProvider.getUserId(token);
                User user = customUserDetailsService.loadUserById(userId);

                if (user.isActive()) {
                    CustomUserDetails principal = CustomUserDetails.builder().user(user).build();
                    Authentication authentication = jwtTokenProvider.getAuthentication(principal);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("비활성 상태({})의 회원이 토큰으로 접근 시도: userId={}", user.getStatus(), userId);
                }
            } else if (status == TokenStatus.EXPIRED) {
                log.debug("만료된 토큰으로 요청: {}", request.getRequestURI());
            }
            // INVALID 토큰은 별도 처리 없이 인증되지 않은 요청으로 흘려보냄 -> 보호된 경로면 401
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
