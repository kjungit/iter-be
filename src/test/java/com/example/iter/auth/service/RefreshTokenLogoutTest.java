package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.response.TokenResponse;
import com.example.iter.auth.exception.RefreshTokenReuseException;
import com.example.iter.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class RefreshTokenLogoutTest {

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

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void revokesOnlyRequestedRefreshToken() {
        User user = saveUser("logout@example.com");
        TokenResponse firstSession = login(user);
        TokenResponse secondSession = login(user);

        refreshTokenService.revoke(user.getId(), firstSession.refreshToken());

        assertThat(findByRawToken(firstSession.refreshToken()).isRevoked()).isTrue();
        assertThat(findByRawToken(secondSession.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void repeatedLogoutIsIdempotent() {
        User user = saveUser("idempotent@example.com");
        String refreshToken = login(user).refreshToken();

        refreshTokenService.revoke(user.getId(), refreshToken);

        assertThatCode(() -> refreshTokenService.revoke(user.getId(), refreshToken))
                .doesNotThrowAnyException();
        assertThat(findByRawToken(refreshToken).isRevoked()).isTrue();
    }

    @Test
    void missingTokenIsIdempotent() {
        User user = saveUser("missing-logout@example.com");

        assertThatCode(() -> refreshTokenService.revoke(user.getId(), "not-stored-refresh-token"))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotRevokeAnotherUsersToken() {
        User owner = saveUser("token-owner@example.com");
        User requester = saveUser("logout-requester@example.com");
        String ownersRefreshToken = login(owner).refreshToken();

        refreshTokenService.revoke(requester.getId(), ownersRefreshToken);

        assertThat(findByRawToken(ownersRefreshToken).isRevoked()).isFalse();
    }

    @Test
    void loggedOutTokenCannotBeRotated() {
        User user = saveUser("logout-rotate@example.com");
        String refreshToken = login(user).refreshToken();
        refreshTokenService.revoke(user.getId(), refreshToken);

        assertThatThrownBy(() -> refreshTokenService.rotate(refreshToken))
                .isInstanceOfSatisfying(RefreshTokenReuseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    private TokenResponse login(User user) {
        return authService.login(new LoginRequest(user.getEmail(), "Password123!"));
    }

    private RefreshToken findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(rawToken)).orElseThrow();
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .build());
    }
}
