package com.example.iter.common.security;

import com.example.iter.auth.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

// 토큰 생성/검증/해석을 전담하는 컴포넌트 (token 프로젝트의 TokenProvider와 동일한 설계를 따름)
//
// token 프로젝트와의 차이점 한 가지:
// - token 프로젝트는 토큰 클레임만으로 User를 복원해서 인증 정보를 만들었다.
// - 이 프로젝트는 클레임에서 사용자 id만 꺼내고, 실제 User는 DB에서 다시 조회한다(JwtAuthenticationFilter 참고).
//   -> 이렇게 해야 "회원 상태에 따른 로그인 제한(정지 등)"이 액세스 토큰 만료 전에도 즉시 반영된다.
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecretKey()));
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build();
    }

    public String generateAccessToken(User user) {
        return generateToken(user, jwtProperties.getAccessTokenValidity(), ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, jwtProperties.getRefreshTokenValidity(), REFRESH_TOKEN_TYPE);
    }

    private String generateToken(User user, Duration validity, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ROLE, user.getRole())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public TokenStatus validateToken(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE);
    }

    public TokenStatus validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE);
    }

    private TokenStatus validateToken(String token, String expectedTokenType) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            if (!expectedTokenType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                return TokenStatus.INVALID;
            }
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰");
            return TokenStatus.EXPIRED;
        } catch (Exception e) {
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            return TokenStatus.INVALID;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public LocalDateTime getExpiration(String token) {
        Date expiration = getClaims(token).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    private Claims getClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    // 이미 조회해 둔 CustomUserDetails로 시큐리티 인증 객체를 만든다.
    public Authentication getAuthentication(CustomUserDetails principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
