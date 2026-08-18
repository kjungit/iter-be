package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findWithLockByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserId(Long userId);

    List<RefreshToken> findAllByFamilyId(String familyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RefreshToken r
            SET r.revokedAt = :revokedAt
            WHERE r.familyId = :familyId
              AND r.revokedAt IS NULL
            """)
    int revokeAllActiveByFamilyId(
            @Param("familyId") String familyId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
