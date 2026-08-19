package com.example.iter.reservation.controller.api;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.reservation.domain.entity.ProductConditionType;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.dto.response.ConditionEvidenceResponse;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
import com.example.iter.reservation.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReturnApiController.class)
@Import(GlobalExceptionHandler.class)
class ReturnApiControllerTest {

    private static final Long OWNER_ID = 1L;
    private static final Long RENTAL_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReturnService returnService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails ownerPrincipal;

    @BeforeEach
    void setUp() {
        ownerPrincipal = principal(OWNER_ID, Role.USER);
    }

    @Test
    void 반납_확인_대상_목록을_페이지로_조회한다() throws Exception {
        ReturnTargetResponse target = new ReturnTargetResponse(
                RENTAL_ID,
                "예약 당시 맥북",
                "https://example.com/thumbnail.jpg",
                new UserSummaryResponse(2L, "대여자닉네임"),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11)
        );
        when(returnService.getReturnTargets(eq(OWNER_ID), any(PagingRequest.class)))
                .thenReturn(new PageResponse<>(List.of(target), 1, 5, 6, 2));

        mockMvc.perform(get("/api/v1/rentals/returns")
                        .with(user(ownerPrincipal))
                        .queryParam("page", "1")
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rentalId").value(RENTAL_ID))
                .andExpect(jsonPath("$.content[0].equipmentName").value("예약 당시 맥북"))
                .andExpect(jsonPath("$.content[0].renter.userId").value(2))
                .andExpect(jsonPath("$.content[0].renter.nickName").value("대여자닉네임"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));

        ArgumentCaptor<PagingRequest> requestCaptor = ArgumentCaptor.forClass(PagingRequest.class);
        verify(returnService).getReturnTargets(eq(OWNER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isEqualTo(1);
        assertThat(requestCaptor.getValue().size()).isEqualTo(5);
    }

    @Test
    void 페이징_조건이_없으면_기본값으로_반납_확인_대상을_조회한다() throws Exception {
        when(returnService.getReturnTargets(eq(OWNER_ID), any(PagingRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/rentals/returns")
                        .with(user(ownerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        ArgumentCaptor<PagingRequest> requestCaptor = ArgumentCaptor.forClass(PagingRequest.class);
        verify(returnService).getReturnTargets(eq(OWNER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().page()).isZero();
        assertThat(requestCaptor.getValue().size()).isEqualTo(20);
    }

    @Test
    void 수령과_반납_증빙을_비교_조회한다() throws Exception {
        when(returnService.getReturnComparison(OWNER_ID, RENTAL_ID))
                .thenReturn(comparisonResponse());

        mockMvc.perform(get("/api/v1/rentals/{rentalId}/return-comparison", RENTAL_ID)
                        .with(user(ownerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(RENTAL_ID))
                .andExpect(jsonPath("$.equipmentName").value("예약 당시 맥북"))
                .andExpect(jsonPath("$.receipt.productCondition").value("NORMAL"))
                .andExpect(jsonPath("$.receipt.imageUrls[0]").value("receipt.jpg"))
                .andExpect(jsonPath("$.returnReceipt.productCondition").value("DAMAGED"))
                .andExpect(jsonPath("$.returnReceipt.imageUrls[0]").value("return.jpg"));

        verify(returnService).getReturnComparison(OWNER_ID, RENTAL_ID);
    }

    @Test
    void 정상_반납을_최종_확인한다() throws Exception {
        when(returnService.confirmReturn(eq(OWNER_ID), eq(RENTAL_ID), any(ReturnConfirmationRequest.class)))
                .thenReturn(new ReturnConfirmationResponse(RENTAL_ID, RentalStatus.COMPLETED, null));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hasIssue": false,
                                  "disputeReason": null,
                                  "disputeDescription": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(RENTAL_ID))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.disputeId").doesNotExist());

        ArgumentCaptor<ReturnConfirmationRequest> requestCaptor =
                ArgumentCaptor.forClass(ReturnConfirmationRequest.class);
        verify(returnService).confirmReturn(eq(OWNER_ID), eq(RENTAL_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().hasIssue()).isFalse();
    }

    @Test
    void 비정상_반납을_최종_확인하면_분쟁_ID를_반환한다() throws Exception {
        when(returnService.confirmReturn(eq(OWNER_ID), eq(RENTAL_ID), any(ReturnConfirmationRequest.class)))
                .thenReturn(new ReturnConfirmationResponse(RENTAL_ID, RentalStatus.DISPUTED, 50L));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hasIssue": true,
                                  "disputeReason": "모서리 파손",
                                  "disputeDescription": "반납된 장비의 모서리가 파손되었습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"))
                .andExpect(jsonPath("$.disputeId").value(50));
    }

    @Test
    void 상품_이상_여부가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(returnService, never()).confirmReturn(any(), any(), any());
    }

    @Test
    void 비정상_반납인데_분쟁_입력이_부족하면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hasIssue": true,
                                  "disputeReason": "파손"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(returnService, never()).confirmReturn(any(), any(), any());
    }

    @Test
    void 정상_반납인데_분쟁_입력이_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hasIssue": false,
                                  "disputeReason": "파손",
                                  "disputeDescription": "파손 설명"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(returnService, never()).confirmReturn(any(), any(), any());
    }

    @Test
    void 페이지_크기가_허용_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/returns")
                        .with(user(ownerPrincipal))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(returnService, never()).getReturnTargets(any(), any());
    }

    @Test
    void 대여_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/{rentalId}/return-comparison", 0L)
                        .with(user(ownerPrincipal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(returnService, never()).getReturnComparison(any(), any());
    }

    @Test
    void 거래_당사자가_아니면_증빙_비교시_403을_반환한다() throws Exception {
        when(returnService.getReturnComparison(OWNER_ID, RENTAL_ID))
                .thenThrow(new CustomException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/rentals/{rentalId}/return-comparison", RENTAL_ID)
                        .with(user(ownerPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 이미_확인된_반납을_다시_확인하면_409를_반환한다() throws Exception {
        when(returnService.confirmReturn(eq(OWNER_ID), eq(RENTAL_ID), any(ReturnConfirmationRequest.class)))
                .thenThrow(new CustomException(ErrorCode.RETURN_ALREADY_CONFIRMED));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-confirmation", RENTAL_ID)
                        .with(user(ownerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hasIssue": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_ALREADY_CONFIRMED"));
    }

    @Test
    void 인증이_없으면_반납_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/rentals/returns"))
                .andExpect(status().isUnauthorized());

        verify(returnService, never()).getReturnTargets(any(), any());
    }

    private ReturnComparisonResponse comparisonResponse() {
        return new ReturnComparisonResponse(
                RENTAL_ID,
                "예약 당시 맥북",
                new UserSummaryResponse(2L, "대여자닉네임"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11),
                new ConditionEvidenceResponse(
                        ProductConditionType.NORMAL,
                        "수령 시 정상",
                        List.of("receipt.jpg"),
                        LocalDateTime.of(2026, 8, 1, 14, 30)
                ),
                new ConditionEvidenceResponse(
                        ProductConditionType.DAMAGED,
                        "반납 시 파손",
                        List.of("return.jpg"),
                        LocalDateTime.of(2026, 8, 11, 17, 20)
                )
        );
    }

    private CustomUserDetails principal(Long userId, Role role) {
        return CustomUserDetails.builder()
                .user(User.builder()
                        .id(userId)
                        .email("owner@iter.test")
                        .password("encoded-password")
                        .name("등록자")
                        .nickname("등록자닉네임")
                        .role(role)
                        .build())
                .build();
    }
}
