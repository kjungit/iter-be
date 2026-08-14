package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.OAuthAccount;
import com.example.iter.auth.domain.entity.OAuthProvider;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.OAuthAccountRepository;
import com.example.iter.auth.domain.repository.OAuthPendingTokenRepository;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.service.OAuthPendingTokenService;
import com.example.iter.auth.service.model.KakaoUserInfo;
import com.example.iter.common.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpSession;
import com.example.iter.auth.support.OAuth2ExchangeSessionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class OAuth2FlowApiTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String REFRESH_COOKIE_NAME = "iter-refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuthPendingTokenService pendingTokenService;

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
    void authorizationEndpointRedirectsToKakaoWithBackendCallbackAndState() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        startsWith("https://kauth.kakao.com/oauth/authorize")
                ))
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        containsString("client_id=test-kakao-client-id")
                ))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("state=")))
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        containsString("redirect_uri=http://localhost/login/oauth2/code/kakao")
                ))
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        containsString("code_challenge_method=S256")
                ));
    }

    @Test
    void linkedUserExchangeReturnsAccessBodyAndRefreshCookie() throws Exception {
        User user = saveUser("oauth-api-linked@example.com");
        oAuthAccountRepository.saveAndFlush(OAuthAccount.builder()
                .userId(user.getId())
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-api-100")
                .build());
        String code = issueExchange("kakao-api-100", user.getEmail());
        MockHttpSession session = sessionWithExchangeCode(code);

        MvcResult result = mockMvc.perform(withCsrf(exchangeRequest(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
    }

    @Test
    void newUserExchangeReturnsSignupActionWithoutRefreshCookie() throws Exception {
        String code = issueExchange("kakao-api-200", "oauth-api-new@example.com");
        MockHttpSession session = sessionWithExchangeCode(code);

        MvcResult result = mockMvc.perform(withCsrf(exchangeRequest(session)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.action").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.oauthToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andReturn();

        assertThat(result.getResponse().getCookie(REFRESH_COOKIE_NAME)).isNull();
    }

    @Test
    void signupCreatesOAuthUserAndLogsIn() throws Exception {
        String code = issueExchange("kakao-api-300", "oauth-api-signup@example.com");
        MvcResult exchangeResult = mockMvc.perform(withCsrf(
                        exchangeRequest(sessionWithExchangeCode(code))))
                .andExpect(status().isAccepted())
                .andReturn();
        String oauthToken = com.jayway.jsonpath.JsonPath.read(
                exchangeResult.getResponse().getContentAsString(),
                "$.oauthToken"
        );

        MvcResult signupResult = mockMvc.perform(withCsrf(signupRequest(
                        oauthToken,
                        "oauth-api-signup@example.com"
                )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertThat(signupResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).isNotNull();
        assertThat(userRepository.findByEmail("oauth-api-signup@example.com").orElseThrow().getPassword())
                .isNull();
    }

    @Test
    void exchangeWithoutCsrfTokenIsForbidden() throws Exception {
        String code = issueExchange("kakao-api-400", null);
        MockHttpSession session = sessionWithExchangeCode(code);

        mockMvc.perform(exchangeRequest(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(pendingTokenRepository.findAll().getFirst().isUsed()).isFalse();
    }

    @Test
    void exchangeWithoutOAuthSessionIsUnauthorized() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/oauth2/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("OAUTH_TOKEN_INVALID"));
    }

    @Test
    void signupWithoutCsrfTokenHasNoSideEffects() throws Exception {
        String oauthToken = issueSignupActionToken(
                "kakao-api-500",
                "oauth-api-no-csrf@example.com"
        );

        mockMvc.perform(signupRequest(oauthToken, "oauth-api-no-csrf@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(userRepository.findByEmail("oauth-api-no-csrf@example.com")).isEmpty();
        assertThat(oAuthAccountRepository.count()).isZero();
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void signupWithForgedCsrfTokenHasNoSideEffects() throws Exception {
        String oauthToken = issueSignupActionToken(
                "kakao-api-600",
                "oauth-api-forged-csrf@example.com"
        );
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(signupRequest(oauthToken, "oauth-api-forged-csrf@example.com")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue() + "forged"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(userRepository.findByEmail("oauth-api-forged-csrf@example.com")).isEmpty();
        assertThat(oAuthAccountRepository.count()).isZero();
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void linkEndpointRequiresAuthentication() throws Exception {
        User targetUser = saveUser("oauth-api-link-auth@example.com");
        String oauthToken = issueSignupActionToken("kakao-api-700", targetUser.getEmail());

        mockMvc.perform(linkRequest(null, oauthToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(oAuthAccountRepository.count()).isZero();
    }

    @Test
    void linkEndpointConnectsKakaoAccountToAuthenticatedTargetUser() throws Exception {
        User targetUser = saveUser("oauth-api-link@example.com");
        String oauthToken = issueSignupActionToken("kakao-api-800", targetUser.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(targetUser);

        mockMvc.perform(linkRequest(accessToken, oauthToken))
                .andExpect(status().isNoContent());

        OAuthAccount linkedAccount = oAuthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "kakao-api-800"
        ).orElseThrow();
        assertThat(linkedAccount.getUserId()).isEqualTo(targetUser.getId());
    }

    private MockHttpServletRequestBuilder exchangeRequest(MockHttpSession session) {
        return post("/api/v1/auth/oauth2/kakao/exchange")
                .session(session);
    }

    private MockHttpServletRequestBuilder signupRequest(String oauthToken, String email) {
        return post("/api/v1/auth/oauth2/kakao/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "oauthToken":"%s",
                          "email":"%s",
                          "name":"홍길동",
                          "nickname":"길동",
                          "phone":"010-1234-5678"
                        }
                        """.formatted(oauthToken, email));
    }

    private MockHttpServletRequestBuilder linkRequest(String accessToken, String oauthToken) {
        MockHttpServletRequestBuilder request = post("/api/v1/users/me/oauth2/kakao/link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"oauthToken":"%s"}
                        """.formatted(oauthToken));
        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return request;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        Cookie csrfCookie = getCsrfCookie();
        return request.cookie(csrfCookie).header(CSRF_HEADER_NAME, csrfCookie.getValue());
    }

    private Cookie getCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    private MockHttpSession sessionWithExchangeCode(String code) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAuth2ExchangeSessionManager.ATTRIBUTE_NAME, code);
        return session;
    }

    private String issueSignupActionToken(String providerUserId, String email) throws Exception {
        String code = issueExchange(providerUserId, email);
        MvcResult exchangeResult = mockMvc.perform(withCsrf(
                        exchangeRequest(sessionWithExchangeCode(code))))
                .andExpect(status().isAccepted())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                exchangeResult.getResponse().getContentAsString(),
                "$.oauthToken"
        );
    }

    private String issueExchange(String providerUserId, String email) {
        return pendingTokenService.issueLoginExchange(new KakaoUserInfo(
                providerUserId,
                email,
                "카카오닉네임"
        ));
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동")
                .phone("010-1234-5678")
                .build());
    }
}
