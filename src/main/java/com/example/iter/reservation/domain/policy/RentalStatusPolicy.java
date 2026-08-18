package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.util.EnumSet;
import java.util.Set;

public final class RentalStatusPolicy {

    private static final Set<RentalStatus> WITHDRAWAL_BLOCKING_STATUSES = Set.copyOf(
            EnumSet.complementOf(EnumSet.of(
                    RentalStatus.COMPLETED,
                    RentalStatus.REJECTED,
                    RentalStatus.CANCELED
            ))
    );

    private RentalStatusPolicy() {
    }

    public static Set<RentalStatus> withdrawalBlockingStatuses() {
        return WITHDRAWAL_BLOCKING_STATUSES;
    }
}
