package com.example.iter.auth.service;

import com.example.iter.auth.config.OAuth2FlowProperties;
import com.example.iter.auth.domain.entity.OAuthPendingPurpose;
import com.example.iter.auth.domain.entity.OAuthPendingToken;
import com.example.iter.auth.domain.entity.OAuthProvider;
import com.example.iter.auth.domain.repository.OAuthPendingTokenRepository;
import com.example.iter.auth.service.model.ConsumedOAuthToken;
import com.example.iter.auth.service.model.IssuedOAuthPendingToken;
import com.example.iter.auth.service.model.KakaoUserInfo;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OAuthPendingTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthPendingTokenRepository pendingTokenRepository;
    private final OAuthPendingTokenHasher tokenHasher;
    private final OAuth2FlowProperties properties;

    @Transactional
    public String issueLoginExchange(KakaoUserInfo userInfo) {
        return issue(
                userInfo,
                OAuthPendingPurpose.LOGIN_EXCHANGE,
                null,
                properties.getExchangeTokenValidity()
        ).rawToken();
    }

    @Transactional
    public ConsumedOAuthToken consumeLoginExchange(String rawToken) {
        return consume(rawToken, OAuthPendingPurpose.LOGIN_EXCHANGE);
    }

    @Transactional
    public IssuedOAuthPendingToken issueActionToken(
            ConsumedOAuthToken source,
            Long targetUserId
    ) {
        KakaoUserInfo userInfo = new KakaoUserInfo(
                source.providerUserId(),
                source.email(),
                source.nickname()
        );
        return issue(
                userInfo,
                OAuthPendingPurpose.SIGNUP_OR_LINK,
                targetUserId,
                properties.getActionTokenValidity()
        );
    }

    @Transactional
    public ConsumedOAuthToken consumeActionToken(String rawToken) {
        return consume(rawToken, OAuthPendingPurpose.SIGNUP_OR_LINK);
    }

    private IssuedOAuthPendingToken issue(
            KakaoUserInfo userInfo,
            OAuthPendingPurpose purpose,
            Long targetUserId,
            Duration validity
    ) {
        pendingTokenRepository.deleteExpiredBefore(
                LocalDateTime.now().minus(properties.getPendingTokenRetention())
        );
        String rawToken = generateRawToken();
        OAuthPendingToken pendingToken = OAuthPendingToken.builder()
                .tokenHash(tokenHasher.hash(rawToken))
                .provider(OAuthProvider.KAKAO)
                .providerUserId(userInfo.providerUserId())
                .purpose(purpose)
                .email(userInfo.email())
                .nickname(userInfo.nickname())
                .targetUserId(targetUserId)
                .expiresAt(LocalDateTime.now().plus(validity))
                .build();
        pendingTokenRepository.save(pendingToken);
        return new IssuedOAuthPendingToken(rawToken, validity.toSeconds());
    }

    private ConsumedOAuthToken consume(String rawToken, OAuthPendingPurpose expectedPurpose) {
        OAuthPendingToken pendingToken;
        try {
            pendingToken = pendingTokenRepository.findWithLockByTokenHash(tokenHasher.hash(rawToken))
                    .orElseThrow(() -> new CustomException(ErrorCode.OAUTH_TOKEN_INVALID));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        if (pendingToken.getPurpose() != expectedPurpose) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
        }
        if (pendingToken.isUsed()) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (pendingToken.isExpired(now)) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_EXPIRED);
        }

        ConsumedOAuthToken consumedToken = new ConsumedOAuthToken(
                pendingToken.getProvider(),
                pendingToken.getProviderUserId(),
                pendingToken.getEmail(),
                pendingToken.getNickname(),
                pendingToken.getTargetUserId()
        );
        pendingToken.use(now);
        return consumedToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
