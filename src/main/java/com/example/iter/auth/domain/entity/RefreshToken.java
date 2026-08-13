package com.example.iter.auth.domain.entity;

import com.example.iter.common.entity.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_token_family_id", columnList = "family_id"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshToken extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActive(LocalDateTime now) {
        return !isRevoked() && !isExpired(now);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.lastUsedAt = usedAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    public void rotateTo(Long replacementTokenId, LocalDateTime usedAt) {
        if (replacementTokenId == null) {
            throw new IllegalArgumentException("교체 토큰 ID는 필수입니다.");
        }
        if (isRevoked()) {
            throw new IllegalStateException("이미 폐기된 Refresh Token입니다.");
        }

        this.lastUsedAt = usedAt;
        this.revokedAt = usedAt;
        this.replacedByTokenId = replacementTokenId;
    }
}
