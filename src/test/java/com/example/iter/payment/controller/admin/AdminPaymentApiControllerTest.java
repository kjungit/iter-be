package com.example.iter.payment.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.dto.request.AdminPaymentSearchRequest;
import com.example.iter.payment.dto.response.AdminPaymentDetailResponse;
import com.example.iter.payment.dto.response.AdminPaymentRentalResponse;
import com.example.iter.payment.dto.response.AdminPaymentSummaryResponse;
import com.example.iter.payment.service.AdminPaymentQueryService;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPaymentApiController.class)
@Import({
        GlobalExceptionHandler.class,
        RestApiSecurityTestConfig.class,
        AdminPaymentApiControllerTest.MethodSecurityTestConfig.class
})
class AdminPaymentApiControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long PAYMENT_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPaymentQueryService adminPaymentQueryService;

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
    void 관리자가_결제_목록을_조건과_커서로_조회한다() throws Exception {
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(summary()), "next-cursor", true, 20));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", "맥북")
                        .queryParam("status", "PAID")
                        .queryParam("fromDate", "2026-08-01")
                        .queryParam("toDate", "2026-08-31")
                        .queryParam("cursor", "current-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.content[0].rentalId").value(20L))
                .andExpect(jsonPath("$.content[0].orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.content[0].equipmentName").value("맥북 프로"))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.totalPages").doesNotExist());

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("맥북");
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(captor.getValue().fromDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(captor.getValue().toDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(captor.getValue().cursor()).isEqualTo("current-cursor");
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 검색조건을_생략하면_첫_커서와_기본_크기를_사용한다() throws Exception {
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, 20));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().keyword()).isNull();
        assertThat(captor.getValue().status()).isNull();
        assertThat(captor.getValue().fromDate()).isNull();
        assertThat(captor.getValue().toDate()).isNull();
        assertThat(captor.getValue().cursor()).isNull();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 검색어가_정확히_100자이면_조회할_수_있다() throws Exception {
        String boundaryKeyword = "가".repeat(100);
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, 20));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", boundaryKeyword))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().keyword()).hasSize(100);
    }

    @Test
    void 검색어가_100자를_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "100"})
    void 페이지_크기가_허용_경계값이면_조회할_수_있다(String size) throws Exception {
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, Integer.parseInt(size)));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("size", size))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(Integer.parseInt(size));
    }

    @Test
    void 관리자가_결제_상세를_조회한다() throws Exception {
        when(adminPaymentQueryService.getPayment(PAYMENT_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/admin/payments/{paymentId}", PAYMENT_ID)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.payment.renterEmail").value("renter@iter.test"))
                .andExpect(jsonPath("$.rental.equipmentId").value(30L))
                .andExpect(jsonPath("$.rental.dailyPrice").value(30000))
                .andExpect(jsonPath("$.rental.rentalDays").value(10));
    }

    @Test
    void USER_권한으로는_관리자_결제_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 인증이_없으면_관리자_결제_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments"))
                .andExpect(status().isUnauthorized());

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 시작일이_종료일보다_늦으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("fromDate", "2026-08-31")
                        .queryParam("toDate", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 시작일과_종료일이_같으면_조회할_수_있다() throws Exception {
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, 20));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("fromDate", "2026-08-20")
                        .queryParam("toDate", "2026-08-20"))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().fromDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(captor.getValue().toDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101"})
    void 페이지_크기가_허용_범위를_벗어나면_400을_반환한다(String size) throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 커서가_200자를_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("cursor", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 페이지_크기가_숫자가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("size", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 결제_상태가_존재하지_않는_ENUM이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026/08/01", "not-a-date", "2026-02-30"})
    void 날짜_형식이_ISO_DATE가_아니면_400을_반환한다(String invalidDate) throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("fromDate", invalidDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 결제_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments/{paymentId}", 0)
                        .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(adminPaymentQueryService, never()).getPayment(any());
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

    private AdminPaymentSummaryResponse summary() {
        return new AdminPaymentSummaryResponse(
                PAYMENT_ID,
                20L,
                "ORDER-001",
                USER_ID,
                "renter@iter.test",
                "대여자",
                "렌터",
                "맥북 프로",
                BigDecimal.valueOf(300000),
                PaymentStatus.PAID,
                RentalStatus.REQUESTED,
                LocalDateTime.of(2026, 8, 20, 10, 30),
                null,
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
    }

    private AdminPaymentDetailResponse detail() {
        return new AdminPaymentDetailResponse(
                summary(),
                new AdminPaymentRentalResponse(
                        30L,
                        "맥북 프로",
                        "LAPTOP",
                        BigDecimal.valueOf(30000),
                        LocalDate.of(2026, 8, 21),
                        LocalDate.of(2026, 8, 30),
                        10,
                        BigDecimal.valueOf(300000),
                        RentalStatus.REQUESTED
                ),
                LocalDateTime.of(2026, 8, 20, 10, 30)
        );
    }
}
