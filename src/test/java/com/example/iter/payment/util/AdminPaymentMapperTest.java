package com.example.iter.payment.util;

import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.service.model.AdminPaymentDetailRow;
import com.example.iter.payment.service.model.AdminPaymentSummaryRow;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPaymentMapperTest {

    private final AdminPaymentMapper adminPaymentMapper = new AdminPaymentMapper();

    @Test
    void 목록_조회_결과를_결제_요약_응답으로_변환한다() {
        AdminPaymentSummaryRow row = new AdminPaymentSummaryRow(
                10L,
                20L,
                "ORDER-001",
                2L,
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

        var response = adminPaymentMapper.toSummary(row);

        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(response.rentalId()).isEqualTo(20L);
        assertThat(response.orderId()).isEqualTo("ORDER-001");
        assertThat(response.renterId()).isEqualTo(2L);
        assertThat(response.renterEmail()).isEqualTo("renter@iter.test");
        assertThat(response.renterName()).isEqualTo("대여자");
        assertThat(response.renterNickname()).isEqualTo("렌터");
        assertThat(response.equipmentName()).isEqualTo("맥북 프로");
        assertThat(response.amount()).isEqualByComparingTo("300000");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.rentalStatus()).isEqualTo(RentalStatus.REQUESTED);
        assertThat(response.refundedAt()).isNull();
    }

    @Test
    void 상세_조회_결과를_결제와_대여_상세_응답으로_변환한다() {
        AdminPaymentDetailRow row = new AdminPaymentDetailRow(
                10L,
                20L,
                "ORDER-001",
                2L,
                "renter@iter.test",
                "대여자",
                "렌터",
                30L,
                "예약 당시 맥북",
                "LAPTOP",
                BigDecimal.valueOf(30000),
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 30),
                10,
                BigDecimal.valueOf(300000),
                BigDecimal.valueOf(300000),
                PaymentStatus.REFUNDED,
                RentalStatus.CANCELED,
                LocalDateTime.of(2026, 8, 20, 10, 30),
                LocalDateTime.of(2026, 8, 20, 11, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 11, 0)
        );

        var response = adminPaymentMapper.toDetail(row);

        assertThat(response.payment().paymentId()).isEqualTo(10L);
        assertThat(response.payment().equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.payment().paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.payment().refundedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 11, 0));
        assertThat(response.rental().equipmentId()).isEqualTo(30L);
        assertThat(response.rental().equipmentName()).isEqualTo("예약 당시 맥북");
        assertThat(response.rental().category()).isEqualTo("LAPTOP");
        assertThat(response.rental().dailyPrice()).isEqualByComparingTo("30000");
        assertThat(response.rental().rentalDays()).isEqualTo(10);
        assertThat(response.rental().totalPrice()).isEqualByComparingTo("300000");
        assertThat(response.rental().rentalStatus()).isEqualTo(RentalStatus.CANCELED);
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 11, 0));
    }
}
