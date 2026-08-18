package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.service.AuthService;
import com.example.iter.auth.service.RefreshTokenHasher;
import com.example.iter.auth.service.model.IssuedTokenPair;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutApiTest {

    private static final String REFRESH_COOKIE_NAME = "iter-refresh";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void logoutRevokesRefreshTokenAndDeletesCookie() throws Exception {
        User user = saveUser("api-logout@example.com");
        IssuedTokenPair tokens = login(user);

        MvcResult result = mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), tokens.refreshToken())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        assertThat(findByRawToken(tokens.refreshToken()).isRevoked()).isTrue();
        Cookie deletedCookie = result.getResponse().getCookie(REFRESH_COOKIE_NAME);
        assertThat(deletedCookie).isNotNull();
        assertThat(deletedCookie.getMaxAge()).isZero();
    }

    @Test
    void repeatedLogoutReturnsNoContent() throws Exception {
        User user = saveUser("api-idempotent@example.com");
        IssuedTokenPair tokens = login(user);

        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), tokens.refreshToken())))
                .andExpect(status().isNoContent());
        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), tokens.refreshToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingRefreshCookieReturnsNoContentAndDeletesCookie() throws Exception {
        User user = saveUser("api-no-cookie@example.com");
        IssuedTokenPair tokens = login(user);

        MvcResult result = mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), null)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getCookie(REFRESH_COOKIE_NAME).getMaxAge()).isZero();
    }

    @Test
    void missingDatabaseTokenReturnsNoContent() throws Exception {
        User user = saveUser("api-missing@example.com");
        IssuedTokenPair tokens = login(user);

        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), "not-stored-refresh-token")))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherUsersRefreshTokenReturnsNoContentWithoutRevokingIt() throws Exception {
        User owner = saveUser("api-owner@example.com");
        User requester = saveUser("api-requester@example.com");
        IssuedTokenPair ownerTokens = login(owner);
        IssuedTokenPair requesterTokens = login(requester);

        mockMvc.perform(withCsrf(logoutRequest(requesterTokens.accessToken(), ownerTokens.refreshToken())))
                .andExpect(status().isNoContent());

        assertThat(findByRawToken(ownerTokens.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void unauthenticatedLogoutReturnsUnauthorized() throws Exception {
        mockMvc.perform(withCsrf(logoutRequest(null, "any-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void forgedAccessTokenReturnsUnauthorized() throws Exception {
        User user = saveUser("api-forged-access@example.com");
        IssuedTokenPair tokens = login(user);

        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken() + "forged", tokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void missingCsrfTokenReturnsForbidden() throws Exception {
        User user = saveUser("api-no-csrf-logout@example.com");
        IssuedTokenPair tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(findByRawToken(tokens.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void suspendedUserCanLogout() throws Exception {
        User user = saveUser("api-suspended-logout@example.com");
        IssuedTokenPair tokens = login(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), tokens.refreshToken())))
                .andExpect(status().isNoContent());

        assertThat(findByRawToken(tokens.refreshToken()).isRevoked()).isTrue();
    }

    @Test
    void deletedUserReturnsUnauthorized() throws Exception {
        User user = saveUser("api-deleted-logout@example.com");
        IssuedTokenPair tokens = login(user);
        user.withdraw();
        userRepository.saveAndFlush(user);

        mockMvc.perform(withCsrf(logoutRequest(tokens.accessToken(), tokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(findByRawToken(tokens.refreshToken()).getRevokedAt()).isNull();
    }

    private MockHttpServletRequestBuilder logoutRequest(String accessToken, String refreshToken) {
        MockHttpServletRequestBuilder request = post("/api/v1/auth/logout");
        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        if (refreshToken != null) {
            request.cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken));
        }
        return request;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        Cookie csrfCookie = getCsrfCookie();
        return request
                .cookie(csrfCookie)
                .header(CSRF_HEADER_NAME, csrfCookie.getValue());
    }

    private Cookie getCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    private IssuedTokenPair login(User user) {
        return authService.login(new LoginRequest(user.getEmail(), "Password123!"));
    }

    private RefreshToken findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(rawToken)).orElseThrow();
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .build());
    }
}
