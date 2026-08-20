package com.example.iter.common.security;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 모든 요청마다 한 번 실행되어, 인증 상태를 복원하는 필터.
// 일반 요청: 1) Authorization: Bearer <token> 헤더에서 토큰 추출 2) 서명/만료 검증
//           3) 유효하면 토큰의 사용자 id로 DB에서 최신 User를 조회해 SecurityContext에 등록
// SSE 구독 요청: 브라우저 EventSource가 Authorization 헤더를 못 보내서, 액세스 토큰 대신
//              /sse-ticket에서 미리 발급받은 단발성 티켓(쿼리 파라미터)으로 인증한다 —
//              액세스 토큰 원문이 URL/로그에 남는 걸 피하기 위함 (SseTicketService 참고).
// 어느 쪽이든 실패하면 인증 없이 통과 -> 보호된 경로는 체인 끝에서 401로 거부됨
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SSE_SUBSCRIBE_PATH = "/api/v1/notifications/subscribe";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    // 필수 의존성으로 직접 주입받으면 notification 모듈을 스캔하지 않는 @WebMvcTest 슬라이스마다 (이 필터를 갖다 쓰는 SecurityConfig를 로드하는 모든 테스트가 해당) 빈을 못 찾아 컨텍스트 로딩부터 깨진다.
    // ObjectProvider로 받으면 빈이 없어도(=SSE 구독을 안 쓰는 컨텍스트) 필터 자체는 문제없이 뜨고 실제로 SSE 티켓 인증이 필요한 순간에만 지연 조회한다.
    private final ObjectProvider<SseTicketResolver> sseTicketResolverProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SSE_SUBSCRIBE_PATH.equals(request.getRequestURI())) {
            authenticateBySseTicket(request);
        } else {
            authenticateByBearerToken(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateByBearerToken(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (token == null) {
            return;
        }

        TokenStatus status = jwtTokenProvider.validateToken(token);
        if (status == TokenStatus.VALID) {
            Long userId = jwtTokenProvider.getUserId(token);
            authenticateAs(userId);
        } else if (status == TokenStatus.EXPIRED) {
            log.debug("만료된 토큰으로 요청: {}", request.getRequestURI());
        }
        // INVALID 토큰은 별도 처리 없이 인증되지 않은 요청으로 흘려보냄 -> 보호된 경로면 401
    }

    private void authenticateBySseTicket(HttpServletRequest request) {
        SseTicketResolver resolver = sseTicketResolverProvider.getIfAvailable();
        if (resolver == null) {
            return;
        }
        String ticket = request.getParameter("ticket");
        resolver.consume(ticket).ifPresentOrElse(
                this::authenticateAs,
                () -> log.debug("유효하지 않거나 만료된 SSE 티켓으로 구독 시도")
        );
    }

    private void authenticateAs(Long userId) {
        User user = customUserDetailsService.loadUserById(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            log.warn("탈퇴한 회원이 접근 시도: userId={}", userId);
            return;
        }
        CustomUserDetails principal = CustomUserDetails.builder().user(user).build();
        Authentication authentication = jwtTokenProvider.getAuthentication(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
