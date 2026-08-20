package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalStatusPolicyTest {

    @Test
    void 완료_거절_취소를_제외한_모든_상태는_회원_탈퇴를_차단한다() {
        assertThat(RentalStatusPolicy.withdrawalBlockingStatuses())
                .containsExactlyInAnyOrder(
                        RentalStatus.PENDING,
                        RentalStatus.REQUESTED,
                        RentalStatus.APPROVED,
                        RentalStatus.SHIPPING,
                        RentalStatus.RECEIVED,
                        RentalStatus.RENTING,
                        RentalStatus.RETURN_REQUESTED,
                        RentalStatus.RETURNING,
                        RentalStatus.RETURNED,
                        RentalStatus.DISPUTED
                )
                .doesNotContain(
                        RentalStatus.COMPLETED,
                        RentalStatus.REJECTED,
                        RentalStatus.CANCELED
                );
    }

    @Test
    void 탈퇴_차단_상태_집합은_외부에서_변경할_수_없다() {
        assertThatThrownBy(() -> RentalStatusPolicy.withdrawalBlockingStatuses()
                .remove(RentalStatus.PENDING))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
