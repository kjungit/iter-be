package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.OAuthPendingToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuthPendingTokenRepository extends JpaRepository<OAuthPendingToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OAuthPendingToken> findWithLockByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM OAuthPendingToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
