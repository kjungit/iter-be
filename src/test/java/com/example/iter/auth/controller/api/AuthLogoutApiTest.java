package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.request.LoginRequest;
import com.example.iter.auth.dto.response.TokenResponse;
import com.example.iter.auth.service.AuthService;
import com.example.iter.auth.service.RefreshTokenHasher;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutApiTest {

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
    void logoutReturnsNoContentAndRevokesRefreshToken() throws Exception {
        User user = saveUser("api-logout@example.com");
        TokenResponse tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(findByRawToken(tokens.refreshToken()).isRevoked()).isTrue();
    }

    @Test
    void repeatedLogoutReturnsNoContent() throws Exception {
        User user = saveUser("api-idempotent@example.com");
        TokenResponse tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingDatabaseTokenReturnsNoContent() throws Exception {
        User user = saveUser("api-missing@example.com");
        TokenResponse tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), "not-stored-refresh-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherUsersRefreshTokenReturnsNoContentWithoutRevokingIt() throws Exception {
        User owner = saveUser("api-owner@example.com");
        User requester = saveUser("api-requester@example.com");
        TokenResponse ownerTokens = login(owner);
        TokenResponse requesterTokens = login(requester);

        mockMvc.perform(logoutRequest(requesterTokens.accessToken(), ownerTokens.refreshToken()))
                .andExpect(status().isNoContent());

        assertThat(findByRawToken(ownerTokens.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void unauthenticatedLogoutReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest("any-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void forgedAccessTokenReturnsUnauthorized() throws Exception {
        User user = saveUser("api-forged-access@example.com");
        TokenResponse tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken() + "forged", tokens.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void blankRefreshTokenReturnsValidationError() throws Exception {
        User user = saveUser("api-blank-logout@example.com");
        TokenResponse tokens = login(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Refresh Token은 필수입니다."));
    }

    @Test
    void suspendedUserCanLogout() throws Exception {
        User user = saveUser("api-suspended-logout@example.com");
        TokenResponse tokens = login(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isNoContent());

        assertThat(findByRawToken(tokens.refreshToken()).isRevoked()).isTrue();
    }

    @Test
    void deletedUserReturnsUnauthorized() throws Exception {
        User user = saveUser("api-deleted-logout@example.com");
        TokenResponse tokens = login(user);
        user.withdraw();
        userRepository.saveAndFlush(user);

        mockMvc.perform(logoutRequest(tokens.accessToken(), tokens.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(findByRawToken(tokens.refreshToken()).getRevokedAt()).isNull();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder logoutRequest(
            String accessToken,
            String refreshToken
    ) {
        return post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequest(refreshToken));
    }

    private String refreshRequest(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private TokenResponse login(User user) {
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
