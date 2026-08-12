package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void savesAndFindsTokenByHashWithLock() {
        RefreshToken saved = refreshTokenRepository.saveAndFlush(createToken(1L, "hash-a", UUID.randomUUID().toString()));

        RefreshToken found = refreshTokenRepository.findWithLockByTokenHash("hash-a").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUserId()).isEqualTo(1L);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void findsTokensByUserAndFamily() {
        String familyId = UUID.randomUUID().toString();
        refreshTokenRepository.save(createToken(1L, "hash-a", familyId));
        refreshTokenRepository.save(createToken(1L, "hash-b", familyId));
        refreshTokenRepository.save(createToken(2L, "hash-c", UUID.randomUUID().toString()));
        refreshTokenRepository.flush();

        assertThat(refreshTokenRepository.findAllByUserId(1L)).hasSize(2);
        assertThat(refreshTokenRepository.findAllByFamilyId(familyId)).hasSize(2);
    }

    private RefreshToken createToken(Long userId, String hash, String familyId) {
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .familyId(familyId)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .deviceInfo("test-device")
                .build();
    }
}
