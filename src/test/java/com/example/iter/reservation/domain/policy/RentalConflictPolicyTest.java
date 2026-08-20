package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RentalConflictPolicyTest {

    @Test
    void 거절_취소_완료_상태는_기간을_점유하지_않는다() {
        assertThat(RentalConflictPolicy.nonOccupyingStatuses())
                .containsExactlyInAnyOrder(
                        RentalStatus.REJECTED,
                        RentalStatus.CANCELED,
                        RentalStatus.COMPLETED
                );
    }

    @Test
    void 승인되지_않은_상태는_확정_예약으로_보지_않는다() {
        assertThat(RentalConflictPolicy.nonConfirmedStatuses())
                .containsExactlyInAnyOrder(
                        RentalStatus.PENDING,
                        RentalStatus.REQUESTED,
                        RentalStatus.REJECTED,
                        RentalStatus.CANCELED
                );
    }
}
