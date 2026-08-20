package com.example.iter.payment.controller.admin;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.dto.response.PageResponse;
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
    void 관리자가_결제_목록을_조건과_페이징으로_조회한다() throws Exception {
        when(adminPaymentQueryService.getPayments(any(AdminPaymentSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(summary()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("keyword", "맥북")
                        .queryParam("status", "PAID")
                        .queryParam("fromDate", "2026-08-01")
                        .queryParam("toDate", "2026-08-31")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.content[0].rentalId").value(20L))
                .andExpect(jsonPath("$.content[0].orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.content[0].equipmentName").value("맥북 프로"))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<AdminPaymentSearchRequest> captor =
                ArgumentCaptor.forClass(AdminPaymentSearchRequest.class);
        verify(adminPaymentQueryService).getPayments(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("맥북");
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(captor.getValue().fromDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(captor.getValue().toDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(captor.getValue().page()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(20);
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
                .andExpect(status().isBadRequest());

        verify(adminPaymentQueryService, never()).getPayments(any());
    }

    @Test
    void 페이지_크기가_허용_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .with(user(adminPrincipal))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest());

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
