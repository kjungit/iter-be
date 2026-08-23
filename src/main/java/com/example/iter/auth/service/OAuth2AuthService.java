package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.OAuthAccount;
import com.example.iter.auth.domain.repository.OAuthAccountRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.response.OAuthAction;
import com.example.iter.auth.dto.response.OAuthActionRequiredResponse;
import com.example.iter.auth.service.model.ConsumedOAuthToken;
import com.example.iter.auth.service.model.IssuedOAuthPendingToken;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.service.model.OAuthExchangeResult;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthService {

    private final OAuthPendingTokenService pendingTokenService;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public OAuthExchangeResult exchange(String exchangeCode) {
        ConsumedOAuthToken pending = pendingTokenService.consumeLoginExchange(exchangeCode);

        return oAuthAccountRepository
                .findByProviderAndProviderUserId(pending.provider(), pending.providerUserId())
                .map(account -> authenticateLinkedUser(account.getUserId()))
                .orElseGet(() -> requireSignupOrLink(pending));
    }

    @Transactional
    public IssuedTokenPair signUp(KakaoSignUpRequest request) {
        ConsumedOAuthToken pending = pendingTokenService.consumeActionToken(request.oauthToken());
        if (pending.targetUserId() != null) {
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID);
        }
        if (oAuthAccountRepository.findByProviderAndProviderUserId(
                pending.provider(), pending.providerUserId()).isPresent()) {
            throw new CustomException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }
        String signupEmail = resolveSignupEmail(pending.email(), request.email());
        if (userRepository.existsByEmail(signupEmail)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user;
        try {
            user = userRepository.saveAndFlush(User.builder()
                    .email(signupEmail)
                    .password(null)
                    .name(request.name())
                    .nickname(request.nickname())
                    .phone(request.phone())
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        saveOAuthAccount(user.getId(), pending);

        log.info("OAuth 회원가입 처리: userId={}, provider={}", user.getId(), pending.provider());

        return authService.issueTokens(user);
    }

    private String resolveSignupEmail(String kakaoEmail, String requestedEmail) {
        if (kakaoEmail == null) {
            return requestedEmail;
        }
        if (!kakaoEmail.equalsIgnoreCase(requestedEmail)) {
            throw new CustomException(ErrorCode.OAUTH_EMAIL_MISMATCH);
        }
        return kakaoEmail;
    }

    @Transactional
    public void link(Long authenticatedUserId, String oauthToken) {
        ConsumedOAuthToken pending = pendingTokenService.consumeActionToken(oauthToken);
        if (pending.targetUserId() == null || !pending.targetUserId().equals(authenticatedUserId)) {
            throw new CustomException(ErrorCode.OAUTH_LINK_TARGET_MISMATCH);
        }
        if (oAuthAccountRepository.findByProviderAndProviderUserId(
                pending.provider(), pending.providerUserId()).isPresent()
                || oAuthAccountRepository.existsByUserIdAndProvider(authenticatedUserId, pending.provider())) {
            throw new CustomException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }

        saveOAuthAccount(authenticatedUserId, pending);
        log.info("OAuth 계정 연결 처리: userId={}, provider={}", authenticatedUserId, pending.provider());
    }

    private OAuthExchangeResult authenticateLinkedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED));
        IssuedTokenPair tokenPair = authService.issueTokens(user);
        log.info("OAuth 로그인 처리: userId={}", userId);
        return new OAuthExchangeResult.Authenticated(tokenPair);
    }

    private OAuthExchangeResult requireSignupOrLink(ConsumedOAuthToken pending) {
        User existingUser = pending.email() == null
                ? null
                : userRepository.findByEmail(pending.email()).orElse(null);
        Long targetUserId = existingUser == null ? null : existingUser.getId();
        OAuthAction action = existingUser == null
                ? OAuthAction.SIGNUP_REQUIRED
                : OAuthAction.LINK_REQUIRED;

        IssuedOAuthPendingToken actionToken = pendingTokenService.issueActionToken(pending, targetUserId);
        OAuthActionRequiredResponse response = new OAuthActionRequiredResponse(
                action,
                actionToken.rawToken(),
                pending.email(),
                pending.nickname(),
                actionToken.expiresIn()
        );
        return new OAuthExchangeResult.ActionRequired(response);
    }

    private void saveOAuthAccount(Long userId, ConsumedOAuthToken pending) {
        try {
            oAuthAccountRepository.saveAndFlush(OAuthAccount.builder()
                    .userId(userId)
                    .provider(pending.provider())
                    .providerUserId(pending.providerUserId())
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }
    }
}
