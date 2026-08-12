package com.example.iter.common.security;

import com.example.iter.auth.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class SecurityFoundationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void passwordIsEncodedWithArgon2() {
        String encoded = passwordEncoder.encode("Password123!");

        assertThat(encoded).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches("Password123!", encoded)).isTrue();
    }

    @Test
    void accessAndRefreshTokensAreDistinguished() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        assertThat(jwtTokenProvider.validateToken(accessToken)).isEqualTo(TokenStatus.VALID);
        assertThat(jwtTokenProvider.validateRefreshToken(accessToken)).isEqualTo(TokenStatus.INVALID);
        assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isEqualTo(TokenStatus.VALID);
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isEqualTo(TokenStatus.INVALID);
        assertThat(jwtTokenProvider.getUserId(accessToken)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getExpiration(refreshToken)).isAfter(jwtTokenProvider.getExpiration(accessToken));
    }
}
