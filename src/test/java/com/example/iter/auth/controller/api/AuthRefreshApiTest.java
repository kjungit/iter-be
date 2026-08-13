package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.response.TokenResponse;
import com.example.iter.auth.service.AuthService;
import com.example.iter.common.security.JwtTokenProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshApiTest {

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
    void refreshReturnsNewTokenPair() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-active@example.com");
        TokenResponse loginTokens = login(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(loginTokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(
                        org.hamcrest.Matchers.not(loginTokens.refreshToken())));
    }

    @Test
    void blankRefreshTokenReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(" ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Refresh Token은 필수입니다."));
    }

    @Test
    void accessTokenReturnsInvalidRefreshToken() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-access@example.com");
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void suspendedUserReturnsForbidden() throws Exception {
        User user = saveUser(UserStatus.ACTIVE, "api-suspended@example.com");
        TokenResponse tokens = login(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(tokens.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_SUSPENDED"));
    }

    private String refreshRequest(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private TokenResponse login(User user) {
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
