package com.example.iter.payment.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentHistoryApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private long equipmentIdSequence = 1_000L;

    @Test
    void 본인의_결제_내역만_최신순으로_조회한다() throws Exception {
        User user = saveUser("history-owner");
        User other = saveUser("history-other");
        Rental firstRental = saveRental(user.getId(), "첫 번째 카메라");
        Payment firstPayment = savePayment(firstRental, PaymentStatus.PAID, "first");
        Rental latestRental = saveRental(user.getId(), "최신 노트북");
        Payment latestPayment = savePayment(latestRental, PaymentStatus.REFUNDED, "latest");
        Rental otherRental = saveRental(other.getId(), "다른 사용자의 장비");
        savePayment(otherRental, PaymentStatus.PAID, "other");

        mockMvc.perform(get("/api/v1/users/me/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].paymentId").value(latestPayment.getId()))
                .andExpect(jsonPath("$.content[0].rentalId").value(latestRental.getId()))
                .andExpect(jsonPath("$.content[0].equipmentId").value(latestRental.getEquipmentId()))
                .andExpect(jsonPath("$.content[0].equipmentName").value("최신 노트북"))
                .andExpect(jsonPath("$.content[0].amount").value(30_000))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.content[0].orderId").value("order-latest"))
                .andExpect(jsonPath("$.content[0].paidAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].refundedAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].paymentKey").doesNotExist())
                .andExpect(jsonPath("$.content[1].paymentId").value(firstPayment.getId()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void 결제_상태로_내역을_필터링한다() throws Exception {
        User user = saveUser("status-filter");
        savePayment(saveRental(user.getId(), "결제 대기 장비"), PaymentStatus.PENDING, "pending");
        Payment paid = savePayment(
                saveRental(user.getId(), "결제 완료 장비"), PaymentStatus.PAID, "paid");
        savePayment(saveRental(user.getId(), "환불 장비"), PaymentStatus.REFUNDED, "refunded");

        mockMvc.perform(get("/api/v1/users/me/payments")
                        .param("status", "PAID")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentId").value(paid.getId()))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void 결제_내역을_페이지로_조회한다() throws Exception {
        User user = saveUser("paging");
        savePayment(saveRental(user.getId(), "장비 1"), PaymentStatus.PAID, "page-1");
        savePayment(saveRental(user.getId(), "장비 2"), PaymentStatus.PAID, "page-2");
        savePayment(saveRental(user.getId(), "장비 3"), PaymentStatus.PAID, "page-3");

        mockMvc.perform(get("/api/v1/users/me/payments")
                        .param("page", "1")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void 잘못된_결제_상태는_400을_반환한다() throws Exception {
        User user = saveUser("invalid-status");

        mockMvc.perform(get("/api/v1/users/me/payments")
                        .param("status", "UNKNOWN")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 잘못된_페이지_조건은_400을_반환한다() throws Exception {
        User user = saveUser("invalid-page");

        mockMvc.perform(get("/api/v1/users/me/payments")
                        .param("page", "-1")
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 인증되지_않은_사용자는_결제_내역을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private User saveUser(String tag) {
        return userRepository.saveAndFlush(User.builder()
                .email(tag + "-" + System.nanoTime() + "@example.com")
                .password("encoded-password")
                .name("결제 내역 사용자")
                .nickname(tag)
                .phone("010-1111-2222")
                .build());
    }

    private Rental saveRental(Long renterId, String equipmentName) {
        return rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(equipmentIdSequence++)
                .renterId(renterId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .productNameSnapshot(equipmentName)
                .categorySnapshot("CAMERA")
                .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                .rentalDays(3)
                .totalPrice(BigDecimal.valueOf(30_000))
                .receiverName("수령인")
                .receiverPhone("010-2222-3333")
                .zipcode("12345")
                .address("서울시 테스트구")
                .detailAddress("101호")
                .status(RentalStatus.REQUESTED)
                .build());
    }

    private Payment savePayment(Rental rental, PaymentStatus status, String suffix) {
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.saveAndFlush(Payment.builder()
                .rentalId(rental.getId())
                .amount(rental.getTotalPrice())
                .status(status)
                .paidAt(status == PaymentStatus.PENDING ? null : now)
                .refundedAt(status == PaymentStatus.REFUNDED ? now : null)
                .orderId("order-" + suffix)
                .paymentKey("payment-key-" + suffix)
                .build());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
