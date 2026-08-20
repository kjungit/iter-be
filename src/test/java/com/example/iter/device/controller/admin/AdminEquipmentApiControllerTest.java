package com.example.iter.device.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.dto.request.AdminEquipmentSearchRequest;
import com.example.iter.device.dto.request.AdminEquipmentStatusRequest;
import com.example.iter.device.dto.response.AdminEquipmentDetailResponse;
import com.example.iter.device.dto.response.AdminEquipmentSummaryResponse;
import com.example.iter.device.dto.response.ImageResponse;
import com.example.iter.device.service.AdminEquipmentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(AdminEquipmentApiController.class)
@Import({
        GlobalExceptionHandler.class,
        RestApiSecurityTestConfig.class,
        AdminEquipmentApiControllerTest.MethodSecurityTestConfig.class
})
class AdminEquipmentApiControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long EQUIPMENT_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminEquipmentService adminEquipmentService;

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
    void 관리자가_장비_목록을_검색하고_페이징해_조회한다() throws Exception {
        AdminEquipmentSummaryResponse summary = new AdminEquipmentSummaryResponse(
                EQUIPMENT_ID,
                "맥북 프로",
                EquipmentCategory.LAPTOP,
                BigDecimal.valueOf(30000),
                EquipmentStatus.ACTIVE,
                new UserSummaryResponse(USER_ID, "등록자"),
                "https://example.com/thumbnail.jpg",
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        when(adminEquipmentService.getEquipments(any(AdminEquipmentSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/admin/equipment")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", "맥북")
                        .queryParam("category", "LAPTOP")
                        .queryParam("status", "ACTIVE")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].equipmentId").value(EQUIPMENT_ID))
                .andExpect(jsonPath("$.content[0].name").value("맥북 프로"))
                .andExpect(jsonPath("$.content[0].category").value("LAPTOP"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].owner.userId").value(USER_ID))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<AdminEquipmentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminEquipmentSearchRequest.class);
        verify(adminEquipmentService).getEquipments(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("맥북");
        assertThat(captor.getValue().category()).isEqualTo("LAPTOP");
        assertThat(captor.getValue().status()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(captor.getValue().page()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 관리자가_장비_상세를_조회한다() throws Exception {
        when(adminEquipmentService.getEquipmentDetail(EQUIPMENT_ID))
                .thenReturn(detailResponse(EquipmentStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/admin/equipment/{equipmentId}", EQUIPMENT_ID)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID))
                .andExpect(jsonPath("$.owner.userId").value(USER_ID))
                .andExpect(jsonPath("$.category").value("LAPTOP"))
                .andExpect(jsonPath("$.name").value("맥북 프로"))
                .andExpect(jsonPath("$.images[0].imageId").value(100L))
                .andExpect(jsonPath("$.images[0].thumbnail").value(true));
    }

    @Test
    void 관리자가_장비를_차단한다() throws Exception {
        when(adminEquipmentService.updateEquipmentStatus(
                eq(ADMIN_ID),
                eq(EQUIPMENT_ID),
                any(AdminEquipmentStatusRequest.class)
        )).thenReturn(detailResponse(EquipmentStatus.SUSPENDED));

        mockMvc.perform(patch("/api/v1/admin/equipment/{equipmentId}/status", EQUIPMENT_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUSPENDED",
                                  "reason": "신고 누적으로 관리자 차단"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(EQUIPMENT_ID))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        ArgumentCaptor<AdminEquipmentStatusRequest> captor =
                ArgumentCaptor.forClass(AdminEquipmentStatusRequest.class);
        verify(adminEquipmentService).updateEquipmentStatus(
                eq(ADMIN_ID),
                eq(EQUIPMENT_ID),
                captor.capture()
        );
        assertThat(captor.getValue().status()).isEqualTo(EquipmentStatus.SUSPENDED);
        assertThat(captor.getValue().reason()).isEqualTo("신고 누적으로 관리자 차단");
    }

    @Test
    void USER_권한으로는_관리자_장비_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/equipment")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(adminEquipmentService, never()).getEquipments(any());
    }

    @Test
    void 인증이_없으면_관리자_장비_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/equipment"))
                .andExpect(status().isUnauthorized());

        verify(adminEquipmentService, never()).getEquipments(any());
    }

    @Test
    void 장비_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/equipment/{equipmentId}", 0)
                        .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(adminEquipmentService, never()).getEquipmentDetail(any());
    }

    @Test
    void 관리자가_MAINTENANCE_상태를_요청하면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/equipment/{equipmentId}/status", EQUIPMENT_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MAINTENANCE",
                                  "reason": "잘못된 관리자 상태 변경"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminEquipmentService, never()).updateEquipmentStatus(any(), any(), any());
    }

    @Test
    void 장비_상태_변경_사유가_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/equipment/{equipmentId}/status", EQUIPMENT_ID)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUSPENDED",
                                  "reason": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminEquipmentService, never()).updateEquipmentStatus(any(), any(), any());
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

    private AdminEquipmentDetailResponse detailResponse(EquipmentStatus status) {
        return new AdminEquipmentDetailResponse(
                EQUIPMENT_ID,
                new UserSummaryResponse(USER_ID, "등록자"),
                EquipmentCategory.LAPTOP,
                "맥북 프로",
                "테스트 장비",
                BigDecimal.valueOf(30000),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                status,
                ProductConditionType.NORMAL,
                "정상",
                List.of(new ImageResponse(
                        100L,
                        "https://example.com/image.jpg",
                        1,
                        true
                )),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );
    }
}
