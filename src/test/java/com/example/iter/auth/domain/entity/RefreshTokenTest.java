package com.example.iter.auth.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 12, 12, 0);

    @Test
    void activeTokenIsNeitherExpiredNorRevoked() {
        RefreshToken token = createToken(now.plusDays(14));

        assertThat(token.isActive(now)).isTrue();
        assertThat(token.isExpired(now)).isFalse();
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void tokenExpiresAtTheExpirationTime() {
        RefreshToken token = createToken(now);

        assertThat(token.isExpired(now)).isTrue();
        assertThat(token.isActive(now)).isFalse();
    }

    @Test
    void rotatingTokenRecordsReplacementAndRevocation() {
        RefreshToken token = createToken(now.plusDays(14));

        token.rotateTo(2L, now);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
        assertThat(token.getLastUsedAt()).isEqualTo(now);
        assertThat(token.getReplacedByTokenId()).isEqualTo(2L);
        assertThat(token.isActive(now)).isFalse();
    }

    @Test
    void revokedTokenCannotBeRotatedAgain() {
        RefreshToken token = createToken(now.plusDays(14));
        token.revoke(now);

        assertThatThrownBy(() -> token.rotateTo(2L, now.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private RefreshToken createToken(LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .userId(1L)
                .tokenHash("hashed-token")
                .familyId("550e8400-e29b-41d4-a716-446655440000")
                .expiresAt(expiresAt)
                .build();
    }
}
