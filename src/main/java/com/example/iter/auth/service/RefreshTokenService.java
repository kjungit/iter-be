package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.response.TokenResponse;
import com.example.iter.auth.exception.RefreshTokenReuseException;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.common.security.TokenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
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

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenResponse rotate(String rawRefreshToken) {
        validateRefreshToken(rawRefreshToken);

        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository.findWithLockByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!currentToken.getUserId().equals(jwtTokenProvider.getUserId(rawRefreshToken))) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeAllActiveByFamilyId(currentToken.getFamilyId(), now);
            log.warn("Refresh Token 재사용 탐지: userId={}, familyId={}",
                    currentToken.getUserId(), currentToken.getFamilyId());
            throw new RefreshTokenReuseException();
        }
        if (currentToken.isExpired(now)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(currentToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        validateUserStatus(user);

        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        RefreshToken replacementToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHasher.hash(newRawRefreshToken))
                .familyId(currentToken.getFamilyId())
                .expiresAt(jwtTokenProvider.getExpiration(newRawRefreshToken))
                .deviceInfo(currentToken.getDeviceInfo())
                .build();

        RefreshToken savedReplacement = refreshTokenRepository.saveAndFlush(replacementToken);
        currentToken.rotateTo(savedReplacement.getId(), now);

        return new TokenResponse(
                jwtTokenProvider.generateAccessToken(user),
                newRawRefreshToken
        );
    }

    private void validateRefreshToken(String rawRefreshToken) {
        TokenStatus status = jwtTokenProvider.validateRefreshToken(rawRefreshToken);
        if (status == TokenStatus.EXPIRED) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (status != TokenStatus.VALID) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
    }
}
