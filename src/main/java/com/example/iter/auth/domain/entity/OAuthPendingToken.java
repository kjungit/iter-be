package com.example.iter.auth.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "oauth_pending_token",
        indexes = {
                @Index(name = "idx_oauth_pending_token_expires_at", columnList = "expires_at"),
                @Index(
                        name = "idx_oauth_pending_token_provider_user",
                        columnList = "provider, provider_user_id"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OAuthPendingToken extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthPendingPurpose purpose;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String nickname;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void use(LocalDateTime usedAt) {
        if (isUsed()) {
            throw new IllegalStateException("이미 사용한 OAuth 일회용 토큰입니다.");
        }
        this.usedAt = usedAt;
        this.email = null;
        this.nickname = null;
    }
}
