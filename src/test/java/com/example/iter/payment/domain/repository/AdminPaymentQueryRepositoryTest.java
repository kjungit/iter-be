package com.example.iter.payment.domain.repository;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.config.JpaConfig;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AdminPaymentQueryRepositoryTest {

    @Autowired
    private AdminPaymentQueryRepository adminPaymentQueryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @ValueSource(strings = {
            "order-001",
            "맥북",
            "renter@iter.test",
            "대여자",
            "렌터"
    })
    void 주문번호_장비명_대여자_정보를_대소문자_구분_없이_검색한다(String keyword) {
        Fixture expected = fixture(
                "renter@iter.test",
                "대여자",
                "렌터",
                "예약 당시 맥북 프로",
                "ORDER-001",
                PaymentStatus.PAID
        );
        fixture(
                "other@iter.test",
                "다른회원",
                "카메라회원",
                "소니 카메라",
                "ORDER-002",
                PaymentStatus.PAID
        );

        var result = adminPaymentQueryRepository.searchForAdmin(
                keyword,
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(result.getContent())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.paymentId()).isEqualTo(expected.payment().getId());
                    assertThat(row.rentalId()).isEqualTo(expected.rental().getId());
                    assertThat(row.renterId()).isEqualTo(expected.renter().getId());
                    assertThat(row.equipmentName()).isEqualTo("예약 당시 맥북 프로");
                });
    }

    @Test
    void 퍼센트와_언더스코어를_와일드카드가_아닌_일반_문자로_검색한다() {
        Fixture expected = fixture(
                "special@iter.test",
                "특수문자회원",
                "특수_회원",
                "할인율 20% 장비",
                "ORDER_%_001",
                PaymentStatus.PAID
        );
        fixture(
                "normal@iter.test",
                "일반회원",
                "일반회원",
                "일반 장비",
                "ORDER-PLAIN-002",
                PaymentStatus.PAID
        );

        var percentResult = adminPaymentQueryRepository.searchForAdmin(
                "%",
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );
        var underscoreResult = adminPaymentQueryRepository.searchForAdmin(
                "_",
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(percentResult.getContent())
                .extracting(row -> row.paymentId())
                .containsExactly(expected.payment().getId());
        assertThat(underscoreResult.getContent())
                .extracting(row -> row.paymentId())
                .containsExactly(expected.payment().getId());
    }

    @Test
    void 결제_상태와_생성일_범위를_함께_적용한다() {
        Fixture expected = fixture(
                "paid@iter.test",
                "결제회원",
                "결제회원",
                "노트북",
                "ORDER-PAID",
                PaymentStatus.PAID
        );
        fixture(
                "failed@iter.test",
                "실패회원",
                "실패회원",
                "카메라",
                "ORDER-FAILED",
                PaymentStatus.FAILED
        );

        var result = adminPaymentQueryRepository.searchForAdmin(
                null,
                PaymentStatus.PAID,
                expected.payment().getCreatedAt().minusSeconds(1),
                expected.payment().getCreatedAt().plusSeconds(1),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(row -> row.paymentId())
                .containsExactly(expected.payment().getId());
    }

    @Test
    void 검색_조건이_없으면_전체_결제를_페이지_단위로_조회한다() {
        Fixture first = fixture(
                "first@iter.test",
                "첫회원",
                "첫회원",
                "첫 장비",
                "ORDER-FIRST",
                PaymentStatus.PENDING
        );
        Fixture second = fixture(
                "second@iter.test",
                "둘회원",
                "둘회원",
                "둘 장비",
                "ORDER-SECOND",
                PaymentStatus.PAID
        );

        var firstPage = adminPaymentQueryRepository.searchForAdmin(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "id"))
        );
        var secondPage = adminPaymentQueryRepository.searchForAdmin(
                null,
                null,
                null,
                null,
                PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(row -> row.paymentId())
                .containsExactly(first.payment().getId());
        assertThat(secondPage.getContent())
                .extracting(row -> row.paymentId())
                .containsExactly(second.payment().getId());
    }

    @Test
    void 결제_ID로_상세에_필요한_결제_대여_대여자_정보를_조회한다() {
        Fixture expected = fixture(
                "detail@iter.test",
                "상세회원",
                "상세닉네임",
                "예약 당시 장비명",
                "ORDER-DETAIL",
                PaymentStatus.REFUNDED
        );

        var result = adminPaymentQueryRepository.findDetailById(expected.payment().getId())
                .orElseThrow();

        assertThat(result.paymentId()).isEqualTo(expected.payment().getId());
        assertThat(result.rentalId()).isEqualTo(expected.rental().getId());
        assertThat(result.renterId()).isEqualTo(expected.renter().getId());
        assertThat(result.renterEmail()).isEqualTo("detail@iter.test");
        assertThat(result.equipmentName()).isEqualTo("예약 당시 장비명");
        assertThat(result.category()).isEqualTo("LAPTOP");
        assertThat(result.dailyPrice()).isEqualByComparingTo("30000");
        assertThat(result.rentalDays()).isEqualTo(10);
        assertThat(result.totalPrice()).isEqualByComparingTo("300000");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.rentalStatus()).isEqualTo(RentalStatus.REQUESTED);
    }

    @Test
    void 존재하지_않는_결제_ID의_상세는_조회되지_않는다() {
        assertThat(adminPaymentQueryRepository.findDetailById(999999L)).isEmpty();
    }

    private Fixture fixture(
            String email,
            String name,
            String nickname,
            String equipmentName,
            String orderId,
            PaymentStatus paymentStatus
    ) {
        User renter = userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .nickname(nickname)
                .phone("010-0000-0000")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        Rental rental = rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(100L + renter.getId())
                .renterId(renter.getId())
                .startDate(LocalDate.of(2026, 8, 21))
                .endDate(LocalDate.of(2026, 8, 30))
                .productNameSnapshot(equipmentName)
                .categorySnapshot("LAPTOP")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(10)
                .totalPrice(BigDecimal.valueOf(300000))
                .status(RentalStatus.REQUESTED)
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .rentalId(rental.getId())
                .amount(BigDecimal.valueOf(300000))
                .status(paymentStatus)
                .orderId(orderId)
                .build());

        return new Fixture(renter, rental, payment);
    }

    private record Fixture(
            User renter,
            Rental rental,
            Payment payment
    ) {
    }
}
