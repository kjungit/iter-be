package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RentalConflictPolicyTest {

    @Test
    void 승인_전과_거절_취소_완료_상태는_기간을_점유하지_않는다() {
        assertThat(RentalConflictPolicy.nonOccupyingStatuses())
                .containsExactlyInAnyOrder(
                        RentalStatus.PENDING,
                        RentalStatus.REQUESTED,
                        RentalStatus.REJECTED,
                        RentalStatus.CANCELED,
                        RentalStatus.COMPLETED
                );
    }
}
