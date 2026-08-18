package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.exception.RefreshTokenReuseException;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtProperties;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.common.security.TokenStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class RefreshTokenRotationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rotatesRefreshTokenWithinSameFamily() {
        User user = saveUser(UserStatus.ACTIVE, "active@example.com");
        IssuedTokenPair loginTokens = login(user);
        RefreshToken oldTokenBeforeRotation = findByRawToken(loginTokens.refreshToken());

        IssuedTokenPair rotatedTokens = refreshTokenService.rotate(loginTokens.refreshToken());

        RefreshToken oldToken = refreshTokenRepository.findById(oldTokenBeforeRotation.getId()).orElseThrow();
        RefreshToken newToken = findByRawToken(rotatedTokens.refreshToken());
        assertThat(rotatedTokens.accessToken()).isNotBlank();
        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(loginTokens.refreshToken());
        assertThat(jwtTokenProvider.validateToken(rotatedTokens.accessToken())).isEqualTo(TokenStatus.VALID);
        assertThat(jwtTokenProvider.validateRefreshToken(rotatedTokens.refreshToken())).isEqualTo(TokenStatus.VALID);
        assertThat(newToken.getFamilyId()).isEqualTo(oldToken.getFamilyId());
        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(oldToken.getLastUsedAt()).isEqualTo(oldToken.getRevokedAt());
        assertThat(oldToken.getReplacedByTokenId()).isEqualTo(newToken.getId());
        assertThat(newToken.getRevokedAt()).isNull();
    }

    @Test
    void rejectsAccessToken() {
        User user = saveUser(UserStatus.ACTIVE, "access@example.com");
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        assertErrorCode(() -> refreshTokenService.rotate(accessToken), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rejectsForgedToken() {
        User user = saveUser(UserStatus.ACTIVE, "forged@example.com");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user) + "forged";

        assertErrorCode(() -> refreshTokenService.rotate(refreshToken), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rejectsRefreshTokenMissingFromDatabase() {
        User user = saveUser(UserStatus.ACTIVE, "missing@example.com");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        assertErrorCode(() -> refreshTokenService.rotate(refreshToken), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rejectsExpiredDatabaseToken() {
        User user = saveUser(UserStatus.ACTIVE, "expired@example.com");
        String rawToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenRepository.saveAndFlush(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHasher.hash(rawToken))
                .familyId(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build());

        assertErrorCode(() -> refreshTokenService.rotate(rawToken), ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void rejectsExpiredJwt() {
        User user = saveUser(UserStatus.ACTIVE, "expired-jwt@example.com");
        String expiredToken = expiredRefreshToken(user);

        assertErrorCode(() -> refreshTokenService.rotate(expiredToken), ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void rejectsSuspendedUser() {
        User user = saveUser(UserStatus.ACTIVE, "suspended@example.com");
        IssuedTokenPair tokens = login(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        assertErrorCode(() -> refreshTokenService.rotate(tokens.refreshToken()), ErrorCode.USER_SUSPENDED);
    }

    @Test
    void rejectsDeletedUser() {
        User user = saveUser(UserStatus.ACTIVE, "deleted@example.com");
        IssuedTokenPair tokens = login(user);
        user.withdraw();
        userRepository.saveAndFlush(user);

        assertErrorCode(() -> refreshTokenService.rotate(tokens.refreshToken()), ErrorCode.USER_DELETED);
    }

    @Test
    void reuseRevokesEveryActiveTokenInFamily() {
        User user = saveUser(UserStatus.ACTIVE, "reuse@example.com");
        IssuedTokenPair loginTokens = login(user);
        IssuedTokenPair rotatedTokens = refreshTokenService.rotate(loginTokens.refreshToken());

        assertThatThrownBy(() -> refreshTokenService.rotate(loginTokens.refreshToken()))
                .isInstanceOfSatisfying(RefreshTokenReuseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        RefreshToken replacement = findByRawToken(rotatedTokens.refreshToken());
        assertThat(replacement.getRevokedAt()).isNotNull();
        assertThat(refreshTokenRepository.findAllByFamilyId(replacement.getFamilyId()))
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    void concurrentRotationAllowsAtMostOneSuccessAndRevokesFamily() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "concurrent@example.com");
        String rawToken = login(user).refreshToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = List.of(
                    executor.submit(() -> rotateAfterSignal(rawToken, ready, start)),
                    executor.submit(() -> rotateAfterSignal(rawToken, ready, start))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = futures.stream()
                    .map(future -> getWithin(future, 10, TimeUnit.SECONDS))
                    .toList();

            assertThat(results).filteredOn(IssuedTokenPair.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(RefreshTokenReuseException.class::isInstance).hasSize(1);
        }

        String familyId = findByRawToken(rawToken).getFamilyId();
        assertThat(refreshTokenRepository.findAllByFamilyId(familyId))
                .allMatch(RefreshToken::isRevoked);
    }

    private Object rotateAfterSignal(String rawToken, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 시작 신호를 기다리지 못했습니다.");
            }
            return refreshTokenService.rotate(rawToken);
        } catch (RefreshTokenReuseException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Object getWithin(Future<Object> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception exception) {
            throw new AssertionError("동시 재발급 요청이 제한 시간 안에 완료되지 않았습니다.", exception);
        }
    }

    private IssuedTokenPair login(User user) {
        return authService.login(new LoginRequest(user.getEmail(), "Password123!"));
    }

    private RefreshToken findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(rawToken)).orElseThrow();
    }

    private String expiredRefreshToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .claim("tokenType", "REFRESH")
                .signWith(
                        Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecretKey())),
                        Jwts.SIG.HS512
                )
                .compact();
    }

    private User saveUser(UserStatus status, String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .status(status)
                .build());
    }

    private void assertErrorCode(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
