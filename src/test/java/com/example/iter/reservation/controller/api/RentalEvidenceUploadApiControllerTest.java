package com.example.iter.reservation.controller.api;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.reservation.dto.response.EvidenceImagePresignResponse;
import com.example.iter.reservation.service.RentalEvidenceUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalEvidenceUploadApiController.class)
@Import({GlobalExceptionHandler.class, RestApiSecurityTestConfig.class})
class RentalEvidenceUploadApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalEvidenceUploadService rentalEvidenceUploadService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("user@iter.test")
                .password("encoded-password")
                .name("사용자")
                .nickname("사용자닉네임")
                .role(Role.USER)
                .build();
        principal = CustomUserDetails.builder().user(user).build();
    }

    @Test
    void presigned_URL을_발급한다() throws Exception {
        when(rentalEvidenceUploadService.createPresignedUploads(any())).thenReturn(
                new EvidenceImagePresignResponse(List.of(new EvidenceImagePresignResponse.Item(
                        "rental-evidence/abc.jpg",
                        "https://example.com/upload",
                        Map.of("Content-Type", "image/jpeg"),
                        "https://example.com/rental-evidence/abc.jpg",
                        LocalDateTime.now().plusMinutes(5)
                ))));

        mockMvc.perform(post("/api/v1/rentals/images/presigned-urls")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "files": [ { "contentType": "image/jpeg" } ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploads[0].objectKey").value("rental-evidence/abc.jpg"));
    }

    @Test
    void 지원하지_않는_Content_Type이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/images/presigned-urls")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "files": [ { "contentType": "application/pdf" } ] }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증이_없으면_접근할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/images/presigned-urls")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
