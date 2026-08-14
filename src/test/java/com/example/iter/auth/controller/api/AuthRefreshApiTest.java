package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.service.AuthService;
import com.example.iter.auth.service.model.IssuedTokenPair;
import com.example.iter.common.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class AuthRefreshApiTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void refreshRotatesCookieAndReturnsOnlyNewAccessToken() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-active@example.com");
        IssuedTokenPair loginTokens = login(user);

        MvcResult result = mockMvc.perform(withCsrf(refreshRequest(loginTokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        Cookie rotatedCookie = result.getResponse().getCookie(REFRESH_COOKIE_NAME);
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(loginTokens.refreshToken());
    }

    @Test
    void missingRefreshCookieReturnsInvalidRefreshToken() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void accessTokenCookieReturnsInvalidRefreshToken() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-access@example.com");
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(withCsrf(refreshRequest(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void suspendedUserReturnsForbidden() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-suspended@example.com");
        IssuedTokenPair tokens = login(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(withCsrf(refreshRequest(tokens.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_SUSPENDED"));
    }

    @Test
    void missingCsrfTokenReturnsForbidden() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-no-csrf@example.com");
        IssuedTokenPair tokens = login(user);

        mockMvc.perform(refreshRequest(tokens.refreshToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void forgedCsrfTokenReturnsForbidden() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-forged-csrf@example.com");
        IssuedTokenPair tokens = login(user);
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(refreshRequest(tokens.refreshToken())
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue() + "forged"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void csrfEndpointIssuesReadableCookie() throws Exception {
        Cookie csrfCookie = getCsrfCookie();

        assertThat(csrfCookie.getValue()).isNotBlank();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
    }

    private MockHttpServletRequestBuilder refreshRequest(String refreshToken) {
        return post("/api/v1/auth/refresh")
                .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken))
                .contentType(MediaType.APPLICATION_JSON);
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

    private User saveUser(UserStatus status, String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name("홍길동")
                .nickname("길동이")
                .phone("010-1234-5678")
                .status(status)
                .build());
    }
}
