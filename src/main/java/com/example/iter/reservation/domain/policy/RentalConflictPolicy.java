package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.util.Set;

/**
 * 예약 기간 충돌을 판단할 때 확정 예약으로 취급하지 않는 상태를 정의합니다.
 */
public final class RentalConflictPolicy {

    private static final Set<RentalStatus> NON_CONFIRMED_STATUSES = Set.of(
            RentalStatus.PENDING,
            RentalStatus.REQUESTED,
            RentalStatus.REJECTED,
            RentalStatus.CANCELED
    );

    private RentalConflictPolicy() {
    }

    public static Set<RentalStatus> nonConfirmedStatuses() {
        return NON_CONFIRMED_STATUSES;
    }
}
