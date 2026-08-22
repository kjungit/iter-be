package com.example.iter.reservation.controller.api;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.common.config.RestApiSecurityTestConfig;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.exception.GlobalExceptionHandler;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.common.security.CustomUserDetailsService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.dto.request.ReceiptCreateRequest;
import com.example.iter.reservation.dto.request.ReturnEvidenceCreateRequest;
import com.example.iter.reservation.dto.request.ShippingRegisterRequest;
import com.example.iter.reservation.dto.response.ReceiptCreateResponse;
import com.example.iter.reservation.dto.response.ReturnEvidenceCreateResponse;
import com.example.iter.reservation.dto.response.ReturnRequestResponse;
import com.example.iter.reservation.dto.response.ShippingRegisterResponse;
import com.example.iter.reservation.service.RentalFulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalFulfillmentApiController.class)
@Import({GlobalExceptionHandler.class, RestApiSecurityTestConfig.class})
class RentalFulfillmentApiControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long RENTAL_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalFulfillmentService rentalFulfillmentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails principal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(USER_ID)
                .email("user@iter.test")
                .password("encoded-password")
                .name("사용자")
                .nickname("사용자닉네임")
                .role(Role.USER)
                .build();
        principal = CustomUserDetails.builder().user(user).build();
    }

    @Test
    void 출고_배송을_등록하면_SHIPPING으로_전환된다() throws Exception {
        when(rentalFulfillmentService.registerShipping(eq(USER_ID), eq(RENTAL_ID), any()))
                .thenReturn(new ShippingRegisterResponse(
                        RENTAL_ID, RentalStatus.SHIPPING, "CJ대한통운", "123456789", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/shipping", RENTAL_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "carrier": "CJ대한통운", "trackingNumber": "123456789" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPING"));
    }

    @Test
    void APPROVED가_아니면_배송_등록시_409를_반환한다() throws Exception {
        when(rentalFulfillmentService.registerShipping(eq(USER_ID), eq(RENTAL_ID), any(ShippingRegisterRequest.class)))
                .thenThrow(new CustomException(ErrorCode.RENTAL_NOT_SHIPPABLE));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/shipping", RENTAL_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "carrier": "CJ대한통운", "trackingNumber": "123456789" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RENTAL_NOT_SHIPPABLE"));
    }

    @Test
    void 수령_증빙을_제출하면_RENTING으로_전환된다() throws Exception {
        when(rentalFulfillmentService.createReceipt(eq(USER_ID), eq(RENTAL_ID), any(ReceiptCreateRequest.class)))
                .thenReturn(new ReceiptCreateResponse(RENTAL_ID, RentalStatus.RENTING, LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/receipt", RENTAL_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCondition": "NORMAL", "imageUrls": ["https://example.com/a.jpg"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RENTING"));
    }

    @Test
    void 반납을_신청하면_RETURN_REQUESTED로_전환된다() throws Exception {
        when(rentalFulfillmentService.requestReturn(eq(USER_ID), eq(RENTAL_ID)))
                .thenReturn(new ReturnRequestResponse(RENTAL_ID, RentalStatus.RETURN_REQUESTED));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-request", RENTAL_ID)
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURN_REQUESTED"));
    }

    @Test
    void 반납_증빙을_제출하면_RETURNED로_전환된다() throws Exception {
        when(rentalFulfillmentService.createReturnEvidence(
                eq(USER_ID), eq(RENTAL_ID), any(ReturnEvidenceCreateRequest.class)))
                .thenReturn(new ReturnEvidenceCreateResponse(RENTAL_ID, RentalStatus.RETURNED, LocalDate.now()));

        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-evidence", RENTAL_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCondition": "NORMAL", "imageUrls": ["https://example.com/b.jpg"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void 인증이_없으면_배송_등록에_접근할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{rentalId}/shipping", RENTAL_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
