package com.example.iter.reservation.util;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalOverduePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Test
    void 연체로_판단할_수_있는_상태_목록을_반환한다() {
        assertThat(RentalOverduePolicy.statuses())
                .containsExactlyInAnyOrder(
                        RentalStatus.RECEIVED,
                        RentalStatus.RENTING,
                        RentalStatus.RETURN_REQUESTED,
                        RentalStatus.RETURNING
                );
    }

    @Test
    void 연체_상태_목록은_외부에서_수정할_수_없다() {
        assertThatThrownBy(() -> RentalOverduePolicy.statuses().add(RentalStatus.COMPLETED))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = RentalStatus.class,
            names = {"RECEIVED", "RENTING", "RETURN_REQUESTED", "RETURNING"}
    )
    void 반납이_완료되지_않은_상태에서_종료일이_지나면_연체_일수를_계산한다(RentalStatus status) {
        Rental rental = rental(status, TODAY.minusDays(5));

        assertThat(RentalOverduePolicy.calculateDays(rental, TODAY)).isEqualTo(5);
    }

    @ParameterizedTest
    @EnumSource(
            value = RentalStatus.class,
            names = {"RECEIVED", "RENTING", "RETURN_REQUESTED", "RETURNING"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void 연체_대상_상태가_아니면_종료일이_지나도_0을_반환한다(RentalStatus status) {
        Rental rental = rental(status, TODAY.minusDays(5));

        assertThat(RentalOverduePolicy.calculateDays(rental, TODAY)).isZero();
    }

    @Test
    void 종료일이_오늘이면_연체가_아니다() {
        Rental rental = rental(RentalStatus.RENTING, TODAY);

        assertThat(RentalOverduePolicy.calculateDays(rental, TODAY)).isZero();
    }

    @Test
    void 종료일이_미래이면_연체가_아니다() {
        Rental rental = rental(RentalStatus.RETURNING, TODAY.plusDays(1));

        assertThat(RentalOverduePolicy.calculateDays(rental, TODAY)).isZero();
    }

    private Rental rental(RentalStatus status, LocalDate endDate) {
        return Rental.builder()
                .id(1L)
                .equipmentId(1L)
                .renterId(1L)
                .startDate(endDate.minusDays(2))
                .endDate(endDate)
                .productNameSnapshot("테스트 장비")
                .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                .rentalDays(3)
                .totalPrice(BigDecimal.valueOf(30_000))
                .status(status)
                .build();
    }
}
