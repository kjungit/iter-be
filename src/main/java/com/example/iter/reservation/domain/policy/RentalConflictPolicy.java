package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.util.Set;

/** 대여 생성·조회와 승인 단계에서 사용하는 예약 기간 충돌 정책을 정의합니다. */
public final class RentalConflictPolicy {

    private static final Set<RentalStatus> NON_OCCUPYING_STATUSES = Set.of(
            RentalStatus.REJECTED,
            RentalStatus.CANCELED,
            RentalStatus.COMPLETED
    );

    private static final Set<RentalStatus> NON_CONFIRMED_STATUSES = Set.of(
            RentalStatus.PENDING,
            RentalStatus.REQUESTED,
            RentalStatus.REJECTED,
            RentalStatus.CANCELED
    );

    private static final Set<RentalStatus> NON_SCHEDULED_STATUSES = Set.of(
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

    public static Set<RentalStatus> nonConfirmedStatuses() {
        return NON_CONFIRMED_STATUSES;
    }

    public static Set<RentalStatus> nonScheduledStatuses() {
        return NON_SCHEDULED_STATUSES;
    }
}
