package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.service.RefreshTokenService;
import com.example.iter.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordChangeApiTest {

    private static final String CURRENT_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 비밀번호를_변경하고_모든_Refresh_Token을_폐기한다() throws Exception {
        User user = savePasswordUser();
        refreshTokenService.issueForLogin(user);
        refreshTokenService.issueForLogin(user);

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(CURRENT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, updated.getPassword())).isFalse();

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(user.getId());
        assertThat(tokens).hasSize(2).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void 현재_비밀번호가_틀리면_변경하지_않고_토큰도_유지한다() throws Exception {
        User user = savePasswordUser();
        refreshTokenService.issueForLogin(user);

        mockMvc.perform(patch("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordRequest("WrongPassword!", NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        User unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, unchanged.getPassword())).isTrue();
        assertThat(refreshTokenRepository.findAllByUserId(user.getId()))
                .allMatch(token -> !token.isRevoked());
    }

    @Test
    void 현재와_동일한_비밀번호로는_변경할_수_없다() throws Exception {
        User user = savePasswordUser();

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(CURRENT_PASSWORD, CURRENT_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAME_PASSWORD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("새 비밀번호는 현재 비밀번호와 달라야 합니다."));
    }

    @Test
    void 비밀번호가_없는_OAuth_전용_회원은_일반_비밀번호를_변경할_수_없다() throws Exception {
        User user = userRepository.save(User.builder()
                .email("oauth-only-" + System.nanoTime() + "@example.com")
                .password(null)
                .name("OAuth회원")
                .nickname("oauth")
                .phone("010-1111-2222")
                .build());

        mockMvc.perform(patch("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordRequest(CURRENT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"))
                .andExpect(jsonPath("$.message").value("비밀번호가 설정되어 있지 않은 계정입니다."));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPassword()).isNull();
    }

    @Test
    void 비밀번호_형식과_인증을_검증한다() throws Exception {
        User user = savePasswordUser();

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(CURRENT_PASSWORD, "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(CURRENT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private User savePasswordUser() {
        return userRepository.save(User.builder()
                .email("password-" + System.nanoTime() + "@example.com")
                .password(passwordEncoder.encode(CURRENT_PASSWORD))
                .name("비밀번호회원")
                .nickname("password-user")
                .phone("010-1111-2222")
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    private String passwordRequest(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);
    }
}
