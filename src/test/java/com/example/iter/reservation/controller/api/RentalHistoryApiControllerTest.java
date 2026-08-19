package com.example.iter.reservation.controller.api;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.dto.request.RentalHistorySearchRequest;
import com.example.iter.reservation.dto.response.RentalHistoryResponse;
import com.example.iter.reservation.service.RentalHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalHistoryApiController.class)
@Import(GlobalExceptionHandler.class)
class RentalHistoryApiControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalHistoryService rentalHistoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        principal = principal(Role.USER, UserStatus.ACTIVE);
    }

    @Test
    void 빌린_장비_이력을_검색_조건과_페이지로_조회한다() throws Exception {
        when(rentalHistoryService.getBorrowedHistory(eq(USER_ID), any(RentalHistorySearchRequest.class)))
                .thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/rentals/borrowed")
                        .with(user(principal))
                        .queryParam("status", "COMPLETED")
                        .queryParam("equipmentName", "맥북")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rentalId").value(100L))
                .andExpect(jsonPath("$.content[0].equipmentName").value("예약 당시 맥북"))
                .andExpect(jsonPath("$.content[0].counterparty.userId").value(2L))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        ArgumentCaptor<RentalHistorySearchRequest> requestCaptor =
                ArgumentCaptor.forClass(RentalHistorySearchRequest.class);
        verify(rentalHistoryService).getBorrowedHistory(eq(USER_ID), requestCaptor.capture());

        RentalHistorySearchRequest request = requestCaptor.getValue();
        assertThat(request.status()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(request.equipmentName()).isEqualTo("맥북");
        assertThat(request.page()).isEqualTo(1);
        assertThat(request.size()).isEqualTo(10);
    }

    @Test
    void 빌려준_장비_이력은_로그인_사용자_ID로_조회한다() throws Exception {
        when(rentalHistoryService.getLentHistory(eq(USER_ID), any(RentalHistorySearchRequest.class)))
                .thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/rentals/lent")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rentalId").value(100L));

        ArgumentCaptor<RentalHistorySearchRequest> requestCaptor =
                ArgumentCaptor.forClass(RentalHistorySearchRequest.class);
        verify(rentalHistoryService).getLentHistory(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isZero();
        assertThat(requestCaptor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 빌린_장비_연체_이력을_조회한다() throws Exception {
        when(rentalHistoryService.getBorrowedOverdueHistory(eq(USER_ID), any(PagingRequest.class)))
                .thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/rentals/borrowed/overdue")
                        .with(user(principal))
                        .queryParam("page", "0")
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].overdueDays").value(0));

        ArgumentCaptor<PagingRequest> requestCaptor = ArgumentCaptor.forClass(PagingRequest.class);
        verify(rentalHistoryService).getBorrowedOverdueHistory(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isZero();
        assertThat(requestCaptor.getValue().size()).isEqualTo(5);
    }

    @Test
    void 빌려준_장비_연체_이력을_조회한다() throws Exception {
        when(rentalHistoryService.getLentOverdueHistory(eq(USER_ID), any(PagingRequest.class)))
                .thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/rentals/lent/overdue")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<PagingRequest> requestCaptor = ArgumentCaptor.forClass(PagingRequest.class);
        verify(rentalHistoryService).getLentOverdueHistory(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isZero();
        assertThat(requestCaptor.getValue().size()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/rentals/borrowed",
            "/api/v1/rentals/lent",
            "/api/v1/rentals/borrowed/overdue",
            "/api/v1/rentals/lent/overdue"
    })
    void 인증하지_않으면_대여_이력을_조회할_수_없다(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 페이지_번호가_음수면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/borrowed")
                        .with(user(principal))
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(rentalHistoryService, never()).getBorrowedHistory(any(), any());
    }

    @Test
    void 페이지_크기가_허용_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/lent/overdue")
                        .with(user(principal))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(rentalHistoryService, never()).getLentOverdueHistory(any(), any());
    }

    @Test
    void 장비명_검색어가_100자를_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/borrowed")
                        .with(user(principal))
                        .queryParam("equipmentName", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(rentalHistoryService, never()).getBorrowedHistory(any(), any());
    }

    @Test
    void 정지된_회원도_기존_대여_이력을_조회할_수_있다() throws Exception {
        CustomUserDetails suspendedPrincipal = principal(Role.USER, UserStatus.SUSPENDED);
        when(rentalHistoryService.getBorrowedHistory(eq(USER_ID), any(RentalHistorySearchRequest.class)))
                .thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/rentals/borrowed")
                        .with(user(suspendedPrincipal)))
                .andExpect(status().isOk());

        verify(rentalHistoryService).getBorrowedHistory(eq(USER_ID), any(RentalHistorySearchRequest.class));
    }

    private CustomUserDetails principal(Role role, UserStatus status) {
        User user = User.builder()
                .id(USER_ID)
                .email("history@iter.test")
                .password("encoded-password")
                .name("이력 사용자")
                .nickname("이력닉네임")
                .role(role)
                .status(status)
                .build();

        return CustomUserDetails.builder()
                .user(user)
                .build();
    }

    private PageResponse<RentalHistoryResponse> pageResponse() {
        RentalHistoryResponse response = new RentalHistoryResponse(
                100L,
                10L,
                "예약 당시 맥북",
                "https://example.com/thumbnail.jpg",
                new UserSummaryResponse(2L, "상대방"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                BigDecimal.valueOf(150_000),
                RentalStatus.COMPLETED,
                0
        );

        return new PageResponse<>(List.of(response), 1, 10, 1, 1);
    }
}
