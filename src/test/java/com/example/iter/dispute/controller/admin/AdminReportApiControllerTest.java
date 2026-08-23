package com.example.iter.dispute.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.dispute.dto.request.AdminReportSearchRequest;
import com.example.iter.dispute.dto.request.AdminReportUpdateRequest;
import com.example.iter.dispute.dto.response.AdminReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.service.AdminReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(AdminReportApiController.class)
@Import({
        GlobalExceptionHandler.class,
        RestApiSecurityTestConfig.class,
        AdminReportApiControllerTest.MethodSecurityTestConfig.class
})
class AdminReportApiControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long REPORTER_ID = 3L;
    private static final Long REPORT_ID = 10L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReportService adminReportService;

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
    void 관리자가_신고_목록을_조건과_커서로_조회한다() throws Exception {
        ReportSummaryResponse summary = new ReportSummaryResponse(
                REPORT_ID,
                new UserSummaryResponse(REPORTER_ID, "신고자"),
                ReportTargetType.EQUIPMENT,
                100L,
                "허위 장비",
                ReportStatus.UNDER_REVIEW,
                CREATED_AT
        );
        when(adminReportService.getReports(any(AdminReportSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(summary), "next-cursor", true, 20));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(adminPrincipal))
                        .queryParam("targetType", "EQUIPMENT")
                        .queryParam("status", "UNDER_REVIEW")
                        .queryParam("cursor", "current-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reportId").value(REPORT_ID))
                .andExpect(jsonPath("$.content[0].reporter.userId").value(REPORTER_ID))
                .andExpect(jsonPath("$.content[0].targetType").value("EQUIPMENT"))
                .andExpect(jsonPath("$.content[0].status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.totalPages").doesNotExist());

        ArgumentCaptor<AdminReportSearchRequest> captor = ArgumentCaptor.forClass(AdminReportSearchRequest.class);
        verify(adminReportService).getReports(captor.capture());
        assertThat(captor.getValue().targetType()).isEqualTo(ReportTargetType.EQUIPMENT);
        assertThat(captor.getValue().status()).isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(captor.getValue().cursor()).isEqualTo("current-cursor");
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 검색조건을_생략하면_첫_커서와_기본_크기를_사용한다() throws Exception {
        when(adminReportService.getReports(any(AdminReportSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, 20));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminReportSearchRequest> captor = ArgumentCaptor.forClass(AdminReportSearchRequest.class);
        verify(adminReportService).getReports(captor.capture());
        assertThat(captor.getValue().targetType()).isNull();
        assertThat(captor.getValue().status()).isNull();
        assertThat(captor.getValue().cursor()).isNull();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 관리자가_신고_상세를_조회한다() throws Exception {
        when(adminReportService.getReport(REPORT_ID))
                .thenReturn(detailResponse(ReportStatus.RECEIVED, null));

        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", REPORT_ID)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.reportId").value(REPORT_ID))
                .andExpect(jsonPath("$.report.reporter.userId").value(REPORTER_ID))
                .andExpect(jsonPath("$.report.status").value("RECEIVED"))
                .andExpect(jsonPath("$.adminMemo").doesNotExist());
    }

    @Test
    void 관리자가_신고를_처리_완료한다() throws Exception {
        when(adminReportService.updateReportStatus(
                eq(ADMIN_ID),
                eq(REPORT_ID),
                any(AdminReportUpdateRequest.class)
        )).thenReturn(detailResponse(ReportStatus.RESOLVED, "운영 정책 위반 확인"));

        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}/status", REPORT_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "adminMemo": "운영 정책 위반 확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminMemo").value("운영 정책 위반 확인"));

        ArgumentCaptor<AdminReportUpdateRequest> captor =
                ArgumentCaptor.forClass(AdminReportUpdateRequest.class);
        verify(adminReportService).updateReportStatus(eq(ADMIN_ID), eq(REPORT_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(captor.getValue().adminMemo()).isEqualTo("운영 정책 위반 확인");
    }

    @Test
    void 일반_회원은_관리자_신고_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(adminReportService, never()).getReports(any());
    }

    @Test
    void 인증이_없으면_관리자_신고_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isUnauthorized());

        verify(adminReportService, never()).getReports(any());
    }

    @Test
    void 신고_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", 0)
                        .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(adminReportService, never()).getReport(any());
    }

    @Test
    void 관리자_처리_메모가_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}/status", REPORT_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "adminMemo": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminReportService, never()).updateReportStatus(any(), any(), any());
    }

    @Test
    void 신고_목록의_페이지_크기가_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user(adminPrincipal))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest());

        verify(adminReportService, never()).getReports(any());
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
        return CustomUserDetails.builder().user(user).build();
    }

    private AdminReportDetailResponse detailResponse(ReportStatus status, String adminMemo) {
        LocalDateTime resolvedAt = status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED
                ? LocalDateTime.of(2026, 8, 2, 10, 0)
                : null;
        ReportDetailResponse report = new ReportDetailResponse(
                REPORT_ID,
                new UserSummaryResponse(REPORTER_ID, "신고자"),
                ReportTargetType.USER,
                50L,
                "신고 사유",
                "신고 내용",
                status,
                CREATED_AT,
                resolvedAt
        );
        return new AdminReportDetailResponse(
                report,
                adminMemo,
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );
    }
}
