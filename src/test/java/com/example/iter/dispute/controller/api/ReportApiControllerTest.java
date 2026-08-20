package com.example.iter.dispute.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.dispute.dto.request.ReportCreateRequest;
import com.example.iter.dispute.dto.request.ReportSearchRequest;
import com.example.iter.dispute.dto.response.ReportDetailResponse;
import com.example.iter.dispute.dto.response.ReportSummaryResponse;
import com.example.iter.dispute.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportApiController.class)
@Import({GlobalExceptionHandler.class, RestApiSecurityTestConfig.class})
class ReportApiControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(USER_ID)
                .email("reporter@iter.test")
                .password("encoded-password")
                .name("신고자")
                .nickname("신고자닉네임")
                .build();

        principal = CustomUserDetails.builder()
                .user(user)
                .build();
    }

    @Test
    void 신고를_접수하면_201과_신고_상세를_반환한다() throws Exception {
        ReportDetailResponse response = detailResponse(10L, ReportStatus.RECEIVED);
        when(reportService.createReport(eq(USER_ID), any(ReportCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/reports")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "EQUIPMENT",
                                  "targetId": 100,
                                  "reason": "허위 정보",
                                  "description": "실제 장비 상태가 설명과 다릅니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.reporter.userId").value(USER_ID))
                .andExpect(jsonPath("$.targetType").value("EQUIPMENT"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        ArgumentCaptor<ReportCreateRequest> requestCaptor = ArgumentCaptor.forClass(ReportCreateRequest.class);
        verify(reportService).createReport(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().targetId()).isEqualTo(100L);
        assertThat(requestCaptor.getValue().reason()).isEqualTo("허위 정보");
    }

    @Test
    void 내_신고_목록을_검색_조건과_페이지로_조회한다() throws Exception {
        ReportSummaryResponse summary = new ReportSummaryResponse(
                10L,
                new UserSummaryResponse(USER_ID, "신고자닉네임"),
                ReportTargetType.EQUIPMENT,
                100L,
                "허위 정보",
                ReportStatus.RECEIVED,
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );
        when(reportService.getMyReports(eq(USER_ID), any(ReportSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/reports/me")
                        .with(user(principal))
                        .queryParam("targetType", "EQUIPMENT")
                        .queryParam("status", "RECEIVED")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reportId").value(10))
                .andExpect(jsonPath("$.content[0].targetType").value("EQUIPMENT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<ReportSearchRequest> requestCaptor = ArgumentCaptor.forClass(ReportSearchRequest.class);
        verify(reportService).getMyReports(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().targetType()).isEqualTo(ReportTargetType.EQUIPMENT);
        assertThat(requestCaptor.getValue().status()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(requestCaptor.getValue().page()).isZero();
        assertThat(requestCaptor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 내_신고_상세를_조회한다() throws Exception {
        when(reportService.getMyReport(USER_ID, 10L))
                .thenReturn(detailResponse(10L, ReportStatus.UNDER_REVIEW));

        mockMvc.perform(get("/api/v1/reports/{reportId}", 10L)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        verify(reportService).getMyReport(USER_ID, 10L);
    }

    @Test
    void 필수값이_없거나_신고_대상_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "EQUIPMENT",
                                  "targetId": 0,
                                  "reason": " ",
                                  "description": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(reportService, never()).createReport(any(), any());
    }

    @Test
    void 페이지_크기가_100을_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/reports/me")
                        .with(user(principal))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(reportService, never()).getMyReports(any(), any());
    }

    @Test
    void 처리중인_중복_신고는_409를_반환한다() throws Exception {
        when(reportService.createReport(eq(USER_ID), any(ReportCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.DUPLICATE_ACTIVE_REPORT));

        mockMvc.perform(post("/api/v1/reports")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "EQUIPMENT",
                                  "targetId": 100,
                                  "reason": "허위 정보",
                                  "description": "이미 접수한 대상입니다."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACTIVE_REPORT"));
    }

    @Test
    void 존재하지_않거나_타인의_신고_상세는_404를_반환한다() throws Exception {
        when(reportService.getMyReport(USER_ID, 10L))
                .thenThrow(new CustomException(ErrorCode.REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/reports/{reportId}", 10L)
                        .with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void 인증이_없으면_신고_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/reports/me"))
                .andExpect(status().isUnauthorized());
    }

    private ReportDetailResponse detailResponse(Long reportId, ReportStatus status) {
        return new ReportDetailResponse(
                reportId,
                new UserSummaryResponse(USER_ID, "신고자닉네임"),
                ReportTargetType.EQUIPMENT,
                100L,
                "허위 정보",
                "실제 장비 상태가 설명과 다릅니다.",
                status,
                LocalDateTime.of(2026, 8, 15, 10, 0),
                null
        );
    }
}
