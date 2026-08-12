package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public String issueForLogin(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("저장되지 않은 회원에게 Refresh Token을 발급할 수 없습니다.");
        }

        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .familyId(UUID.randomUUID().toString())
                .expiresAt(jwtTokenProvider.getExpiration(rawRefreshToken))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawRefreshToken;
    }
}
