package com.example.iter.reservation.domain.policy;

import com.example.iter.reservation.domain.entity.RentalStatus;

import java.util.EnumSet;
import java.util.Set;

public final class RentalStatusPolicy {

    private static final Set<RentalStatus> TERMINAL_STATUSES = Set.of(
            RentalStatus.COMPLETED,
            RentalStatus.REJECTED,
            RentalStatus.CANCELED
    );

    private static final Set<RentalStatus> WITHDRAWAL_BLOCKING_STATUSES = Set.copyOf(
            EnumSet.complementOf(EnumSet.of(
                    RentalStatus.COMPLETED,
                    RentalStatus.REJECTED,
                    RentalStatus.CANCELED
            ))
    );

    // DISPUTED는 서비스에서 ACTIVE_DISPUTE_EXISTS로 별도 차단합니다.
    private static final Set<RentalStatus> EQUIPMENT_DELETION_BLOCKING_STATUSES = Set.copyOf(
            EnumSet.complementOf(EnumSet.of(
                    RentalStatus.COMPLETED,
                    RentalStatus.REJECTED,
                    RentalStatus.CANCELED,
                    RentalStatus.DISPUTED
            ))
    );

    private RentalStatusPolicy() {
    }

    public static Set<RentalStatus> terminalStatuses() {
        return TERMINAL_STATUSES;
    }

    public static Set<RentalStatus> withdrawalBlockingStatuses() {
        return WITHDRAWAL_BLOCKING_STATUSES;
    }

    public static Set<RentalStatus> equipmentDeletionBlockingStatuses() {
        return EQUIPMENT_DELETION_BLOCKING_STATUSES;
    }
}
