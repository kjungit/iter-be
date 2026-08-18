package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.util.Set;

/** 예약 기간을 점유하지 않는 종료 전·종료 상태를 정의합니다. */
public final class RentalConflictPolicy {

    private static final Set<RentalStatus> NON_OCCUPYING_STATUSES = Set.of(
            RentalStatus.PENDING,
            RentalStatus.REQUESTED,
            RentalStatus.REJECTED,
            RentalStatus.CANCELED,
            RentalStatus.COMPLETED
    );

    private RentalConflictPolicy() {
    }

    public static Set<RentalStatus> nonOccupyingStatuses() {
        return NON_OCCUPYING_STATUSES;
    }
}
