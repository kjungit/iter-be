package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.common.security.TokenStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

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

    @Test
    void loginStoresOnlyHashedRefreshToken() {
        User user = saveUser(UserStatus.ACTIVE);

        IssuedTokenPair response = authService.login(new LoginRequest(user.getEmail(), "Password123!"));

        String tokenHash = refreshTokenHasher.hash(response.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(jwtTokenProvider.validateRefreshToken(response.refreshToken())).isEqualTo(TokenStatus.VALID);
        assertThat(storedToken.getTokenHash()).isEqualTo(tokenHash).isNotEqualTo(response.refreshToken());
        assertThat(storedToken.getUserId()).isEqualTo(user.getId());
        assertThatCode(() -> UUID.fromString(storedToken.getFamilyId())).doesNotThrowAnyException();
        assertThat(storedToken.getExpiresAt())
                .isEqualTo(jwtTokenProvider.getExpiration(response.refreshToken()));
        assertThat(storedToken.getRevokedAt()).isNull();
        assertThat(storedToken.getReplacedByTokenId()).isNull();
        assertThat(storedToken.getDeviceInfo()).isNull();
    }

    @Test
    void invalidCredentialsDoNotStoreRefreshToken() {
        User user = saveUser(UserStatus.ACTIVE);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "WrongPassword123!")))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).isEmpty();
    }

    @Test
    void suspendedUserDoesNotStoreRefreshToken() {
        User user = saveUser(UserStatus.SUSPENDED);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "Password123!")))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_SUSPENDED));
        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).isEmpty();
    }

    @Test
    void deletedUserDoesNotStoreRefreshToken() {
        User user = saveUser(UserStatus.DELETED);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "Password123!")))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_DELETED));
        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).isEmpty();
    }

    private User saveUser(UserStatus status) {
        return userRepository.saveAndFlush(User.builder()
                .email(status.name().toLowerCase() + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .status(status)
                .build());
    }
}
