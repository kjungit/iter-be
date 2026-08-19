package com.example.iter.auth.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthPendingTokenTest {

    @Test
    void useMarksTokenUsedAndRemovesProfileInformation() {
        LocalDateTime usedAt = LocalDateTime.now();
        OAuthPendingToken token = OAuthPendingToken.builder()
                .tokenHash("hashed-token")
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .purpose(OAuthPendingPurpose.LOGIN_EXCHANGE)
                .email("user@example.com")
                .nickname("카카오닉네임")
                .expiresAt(usedAt.plusMinutes(1))
                .build();

        token.use(usedAt);

        assertThat(token.getUsedAt()).isEqualTo(usedAt);
        assertThat(token.getEmail()).isNull();
        assertThat(token.getNickname()).isNull();
    }

    @Test
    void alreadyUsedTokenCannotBeUsedAgain() {
        LocalDateTime usedAt = LocalDateTime.now();
        OAuthPendingToken token = OAuthPendingToken.builder()
                .tokenHash("hashed-token")
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .purpose(OAuthPendingPurpose.LOGIN_EXCHANGE)
                .expiresAt(usedAt.plusMinutes(1))
                .build();
        token.use(usedAt);

        assertThatThrownBy(() -> token.use(usedAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용한 OAuth 일회용 토큰입니다.");
    }
}
