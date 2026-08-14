package com.example.iter.auth.service;

import com.example.iter.auth.domain.entity.OAuthAccount;
import com.example.iter.auth.domain.entity.OAuthProvider;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.OAuthAccountRepository;
import com.example.iter.auth.domain.repository.OAuthPendingTokenRepository;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.KakaoSignUpRequest;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.response.OAuthAction;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.auth.service.model.KakaoUserInfo;
import com.example.iter.auth.service.model.OAuthExchangeResult;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.common.security.TokenStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class OAuth2AuthServiceTest {

    @Autowired
    private OAuth2AuthService oAuth2AuthService;

    @Autowired
    private OAuthPendingTokenService pendingTokenService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private OAuthPendingTokenRepository pendingTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        pendingTokenRepository.deleteAll();
        oAuthAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void linkedKakaoAccountLogsInWithIterTokens() {
        User user = savePasswordUser("linked@example.com");
        linkAccount(user, "kakao-100");
        String exchangeCode = issueExchange("kakao-100", user.getEmail(), "연동회원");

        OAuthExchangeResult result = oAuth2AuthService.exchange(exchangeCode);

        OAuthExchangeResult.Authenticated authenticated =
                (OAuthExchangeResult.Authenticated) result;
        assertThat(jwtTokenProvider.validateToken(authenticated.tokenPair().accessToken()))
                .isEqualTo(TokenStatus.VALID);
        assertThat(jwtTokenProvider.validateRefreshToken(authenticated.tokenPair().refreshToken()))
                .isEqualTo(TokenStatus.VALID);
        assertThat(refreshTokenRepository.count()).isOne();
    }

    @Test
    void suspendedLinkedUserCannotReceiveIterTokens() {
        User user = savePasswordUser("suspended-oauth@example.com");
        linkAccount(user, "kakao-101");
        user.suspend();
        userRepository.saveAndFlush(user);
        String exchangeCode = issueExchange("kakao-101", user.getEmail(), "정지회원");

        assertCustomError(
                () -> oAuth2AuthService.exchange(exchangeCode),
                ErrorCode.USER_SUSPENDED
        );
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void newKakaoAccountRequiresSignupAndCreatesPasswordlessUser() {
        String exchangeCode = issueExchange("kakao-200", "new@example.com", "신규회원");
        OAuthExchangeResult.ActionRequired actionRequired =
                (OAuthExchangeResult.ActionRequired) oAuth2AuthService.exchange(exchangeCode);

        assertThat(actionRequired.response().action()).isEqualTo(OAuthAction.SIGNUP_REQUIRED);
        assertThat(actionRequired.response().expiresIn()).isEqualTo(300);

        IssuedTokenPair tokenPair = oAuth2AuthService.signUp(new KakaoSignUpRequest(
                actionRequired.response().oauthToken(),
                "new@example.com",
                "홍길동",
                "길동",
                "010-1234-5678"
        ));

        User savedUser = userRepository.findByEmail("new@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNull();
        assertThat(oAuthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO, "kakao-200")).isPresent();
        assertThat(jwtTokenProvider.validateToken(tokenPair.accessToken())).isEqualTo(TokenStatus.VALID);
    }

    @Test
    void sameEmailRequiresLinkAndTokenIsBoundToThatUser() {
        User targetUser = savePasswordUser("existing@example.com");
        User anotherUser = savePasswordUser("another@example.com");
        String exchangeCode = issueExchange("kakao-300", targetUser.getEmail(), "기존회원");
        OAuthExchangeResult.ActionRequired actionRequired =
                (OAuthExchangeResult.ActionRequired) oAuth2AuthService.exchange(exchangeCode);

        assertThat(actionRequired.response().action()).isEqualTo(OAuthAction.LINK_REQUIRED);
        assertCustomError(
                () -> oAuth2AuthService.link(anotherUser.getId(), actionRequired.response().oauthToken()),
                ErrorCode.OAUTH_LINK_TARGET_MISMATCH
        );

        oAuth2AuthService.link(targetUser.getId(), actionRequired.response().oauthToken());

        OAuthAccount linked = oAuthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO, "kakao-300").orElseThrow();
        assertThat(linked.getUserId()).isEqualTo(targetUser.getId());
    }

    @Test
    void exchangeCodeCanOnlyBeUsedOnce() {
        String exchangeCode = issueExchange("kakao-400", null, "일회용");

        oAuth2AuthService.exchange(exchangeCode);

        assertCustomError(
                () -> oAuth2AuthService.exchange(exchangeCode),
                ErrorCode.OAUTH_TOKEN_ALREADY_USED
        );
    }

    @Test
    void concurrentExchangeAllowsOnlyOneConsumption() throws Exception {
        String exchangeCode = issueExchange("kakao-500", null, "동시교환");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = List.of(
                    executor.submit(() -> exchangeAfterSignal(exchangeCode, ready, start)),
                    executor.submit(() -> exchangeAfterSignal(exchangeCode, ready, start))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = futures.stream()
                    .map(this::getWithin)
                    .toList();

            assertThat(results)
                    .filteredOn(OAuthExchangeResult.ActionRequired.class::isInstance)
                    .hasSize(1);
            assertThat(results)
                    .filteredOn(result -> result instanceof CustomException exception
                            && exception.getErrorCode() == ErrorCode.OAUTH_TOKEN_ALREADY_USED)
                    .hasSize(1);
        }
    }

    @Test
    void passwordlessOAuthUserCannotUsePasswordLogin() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("oauth-only@example.com")
                .password(null)
                .name("OAuth회원")
                .nickname("카카오")
                .phone("010-1234-5678")
                .build());

        assertCustomError(
                () -> authService.login(new LoginRequest(user.getEmail(), "Password123!")),
                ErrorCode.INVALID_CREDENTIALS
        );
    }

    @Test
    void signupActionTokenCannotBeReused() {
        String exchangeCode = issueExchange("kakao-600", "once@example.com", "일회가입");
        OAuthExchangeResult.ActionRequired actionRequired =
                (OAuthExchangeResult.ActionRequired) oAuth2AuthService.exchange(exchangeCode);
        KakaoSignUpRequest request = new KakaoSignUpRequest(
                actionRequired.response().oauthToken(),
                "once@example.com",
                "홍길동",
                "길동",
                "010-1234-5678"
        );

        oAuth2AuthService.signUp(request);

        assertCustomError(
                () -> oAuth2AuthService.signUp(request),
                ErrorCode.OAUTH_TOKEN_ALREADY_USED
        );
    }

    private String issueExchange(String providerUserId, String email, String nickname) {
        return pendingTokenService.issueLoginExchange(new KakaoUserInfo(
                providerUserId,
                email,
                nickname
        ));
    }

    private Object exchangeAfterSignal(
            String exchangeCode,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 시작 신호를 기다리지 못했습니다.");
            }
            return oAuth2AuthService.exchange(exchangeCode);
        } catch (CustomException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Object getWithin(Future<Object> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("동시 OAuth 교환 요청이 제한 시간 안에 완료되지 않았습니다.", exception);
        }
    }

    private void linkAccount(User user, String providerUserId) {
        oAuthAccountRepository.saveAndFlush(OAuthAccount.builder()
                .userId(user.getId())
                .provider(OAuthProvider.KAKAO)
                .providerUserId(providerUserId)
                .build());
    }

    private User savePasswordUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동")
                .phone("010-1234-5678")
                .build());
    }

    private void assertCustomError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
