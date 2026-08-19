package com.example.iter.common.audit.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.dto.request.AdminActionSearchRequest;
import com.example.iter.common.audit.dto.response.AdminActionResponse;
import com.example.iter.common.audit.service.AdminActionQueryService;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminActionApiController.class)
@Import({
        GlobalExceptionHandler.class,
        AdminActionApiControllerTest.MethodSecurityTestConfig.class
})
class AdminActionApiControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long ACTION_ID = 100L;
    private static final Long TARGET_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminActionQueryService adminActionQueryService;

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
    void 관리자가_검색_조건과_페이지_정보로_처리_이력을_조회한다() throws Exception {
        AdminActionResponse actionResponse = new AdminActionResponse(
                ACTION_ID,
                ADMIN_ID,
                AdminActionTargetType.EQUIPMENT,
                TARGET_ID,
                AdminActionType.SUSPEND_EQUIPMENT,
                "신고 누적으로 관리자 차단",
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        when(adminActionQueryService.getAdminActions(any(AdminActionSearchRequest.class)))
                .thenReturn(new PageResponse<>(
                        List.of(actionResponse),
                        1,
                        10,
                        21,
                        3
                ));

        mockMvc.perform(get("/api/v1/admin/actions")
                        .with(user(adminPrincipal))
                        .queryParam("targetType", "EQUIPMENT")
                        .queryParam("targetId", TARGET_ID.toString())
                        .queryParam("action", "SUSPEND_EQUIPMENT")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionId").value(ACTION_ID))
                .andExpect(jsonPath("$.content[0].adminId").value(ADMIN_ID))
                .andExpect(jsonPath("$.content[0].targetType").value("EQUIPMENT"))
                .andExpect(jsonPath("$.content[0].targetId").value(TARGET_ID))
                .andExpect(jsonPath("$.content[0].action").value("SUSPEND_EQUIPMENT"))
                .andExpect(jsonPath("$.content[0].reason").value("신고 누적으로 관리자 차단"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-08-01T10:00:00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21));

        ArgumentCaptor<AdminActionSearchRequest> captor =
                ArgumentCaptor.forClass(AdminActionSearchRequest.class);
        verify(adminActionQueryService).getAdminActions(captor.capture());

        AdminActionSearchRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.targetType())
                .isEqualTo(AdminActionTargetType.EQUIPMENT);
        assertThat(capturedRequest.targetId()).isEqualTo(TARGET_ID);
        assertThat(capturedRequest.action())
                .isEqualTo(AdminActionType.SUSPEND_EQUIPMENT);
        assertThat(capturedRequest.page()).isEqualTo(1);
        assertThat(capturedRequest.size()).isEqualTo(10);
    }

    @Test
    void 검색_조건과_페이지_정보를_생략하면_기본값으로_처리_이력을_조회한다() throws Exception {
        when(adminActionQueryService.getAdminActions(any(AdminActionSearchRequest.class)))
                .thenReturn(new PageResponse<>(
                        List.<AdminActionResponse>of(),
                        0,
                        20,
                        0,
                        0
                ));

        mockMvc.perform(get("/api/v1/admin/actions")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<AdminActionSearchRequest> captor =
                ArgumentCaptor.forClass(AdminActionSearchRequest.class);
        verify(adminActionQueryService).getAdminActions(captor.capture());

        AdminActionSearchRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.targetType()).isNull();
        assertThat(capturedRequest.targetId()).isNull();
        assertThat(capturedRequest.action()).isNull();
        assertThat(capturedRequest.page()).isZero();
        assertThat(capturedRequest.size()).isEqualTo(20);
    }

    @Test
    void USER_권한으로는_관리자_처리_이력을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/actions")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(adminActionQueryService, never())
                .getAdminActions(any(AdminActionSearchRequest.class));
    }

    @Test
    void 인증이_없으면_관리자_처리_이력을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/actions"))
                .andExpect(status().isUnauthorized());

        verify(adminActionQueryService, never())
                .getAdminActions(any(AdminActionSearchRequest.class));
    }

    @Test
    void 대상_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/actions")
                        .with(user(adminPrincipal))
                        .queryParam("targetId", "0"))
                .andExpect(status().isBadRequest());

        verify(adminActionQueryService, never())
                .getAdminActions(any(AdminActionSearchRequest.class));
    }

    @ParameterizedTest
    @MethodSource("invalidPaginationParameters")
    void 페이지_정보가_허용_범위를_벗어나면_400을_반환한다(
            String parameterName,
            String parameterValue
    ) throws Exception {
        mockMvc.perform(get("/api/v1/admin/actions")
                        .with(user(adminPrincipal))
                        .queryParam(parameterName, parameterValue))
                .andExpect(status().isBadRequest());

        verify(adminActionQueryService, never())
                .getAdminActions(any(AdminActionSearchRequest.class));
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

    private static Stream<Arguments> invalidPaginationParameters() {
        return Stream.of(
                Arguments.of("page", "-1"),
                Arguments.of("size", "0"),
                Arguments.of("size", "101")
        );
    }
}

