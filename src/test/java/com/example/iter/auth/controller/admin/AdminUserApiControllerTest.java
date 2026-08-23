package com.example.iter.auth.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.dto.request.AdminUserSearchRequest;
import com.example.iter.auth.dto.request.AdminUserStatusRequest;
import com.example.iter.auth.dto.response.AdminUserDetailResponse;
import com.example.iter.auth.dto.response.AdminUserStatusResponse;
import com.example.iter.auth.dto.response.AdminUserSummaryResponse;
import com.example.iter.auth.service.AdminUserService;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserApiController.class)
@Import({
        GlobalExceptionHandler.class,
        RestApiSecurityTestConfig.class,
        AdminUserApiControllerTest.MethodSecurityTestConfig.class
})
class AdminUserApiControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails adminPrincipal;
    private CustomUserDetails userPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = principal(ADMIN_ID, Role.ADMIN);
        userPrincipal = principal(USER_ID, Role.USER);
    }

    @Test
    void 관리자가_회원_목록을_검색하고_커서로_조회한다() throws Exception {
        AdminUserSummaryResponse summary = new AdminUserSummaryResponse(
                USER_ID,
                "user@iter.test",
                "회원",
                "닉네임",
                Role.USER,
                UserStatus.ACTIVE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        when(adminUserService.getUsers(any(AdminUserSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(summary), "next-cursor", true, 20));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", "iter")
                        .queryParam("status", "ACTIVE")
                        .queryParam("cursor", "current-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(USER_ID))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.totalPages").doesNotExist());

        ArgumentCaptor<AdminUserSearchRequest> captor = ArgumentCaptor.forClass(AdminUserSearchRequest.class);
        verify(adminUserService).getUsers(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("iter");
        assertThat(captor.getValue().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(captor.getValue().cursor()).isEqualTo("current-cursor");
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 관리자가_회원_상세와_거래_요약을_조회한다() throws Exception {
        when(adminUserService.getUser(USER_ID)).thenReturn(new AdminUserDetailResponse(
                USER_ID,
                "user@iter.test",
                "회원",
                "닉네임",
                "010-0000-0002",
                Role.USER,
                UserStatus.ACTIVE,
                3,
                4,
                1,
                2,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 2, 10, 0)
        ));

        mockMvc.perform(get("/api/v1/admin/users/{userId}", USER_ID)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.rentedCount").value(3))
                .andExpect(jsonPath("$.lentCount").value(4))
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.reportCount").value(2));
    }

    @Test
    void 관리자가_회원_상태를_변경한다() throws Exception {
        when(adminUserService.updateStatus(eq(ADMIN_ID), eq(USER_ID), any(AdminUserStatusRequest.class)))
                .thenReturn(new AdminUserStatusResponse(USER_ID, UserStatus.SUSPENDED, null));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", USER_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUSPENDED",
                                  "reason": "신고 누적"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(adminUserService).updateStatus(eq(ADMIN_ID), eq(USER_ID), any(AdminUserStatusRequest.class));
    }

    @Test
    void USER_권한으로는_관리자_회원_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(adminUserService, never()).getUsers(any());
    }

    @Test
    void 인증이_없으면_관리자_회원_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 회원_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/{userId}", 0)
                        .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(adminUserService, never()).getUser(any());
    }

    @Test
    void 회원_상태를_DELETED로_변경하거나_사유를_누락하면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", USER_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DELETED",
                                  "reason": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminUserService, never()).updateStatus(any(), any(), any());
    }

    private CustomUserDetails principal(Long id, Role role) {
        User user = User.builder()
                .id(id)
                .email("principal" + id + "@iter.test")
                .password("encoded-password")
                .name("테스트")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        return CustomUserDetails.builder()
                .user(user)
                .build();
    }
}
