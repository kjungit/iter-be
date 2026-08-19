package com.example.iter.reservation.util;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class RentalOverduePolicy {

    private static final Set<RentalStatus> OVERDUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(
                    RentalStatus.RECEIVED,
                    RentalStatus.RENTING,
                    RentalStatus.RETURN_REQUESTED,
                    RentalStatus.RETURNING
            )
    );

    private RentalOverduePolicy() {
    }

    // 아직 반납이 완료되지 않아 연체로 판단할 수 있는 거래 상태를 반환합니다.
    public static Set<RentalStatus> statuses() {
        return OVERDUE_STATUSES;
    }

    // 거래 상태와 종료일을 기준으로 연체 일수를 계산하고, 연체가 아니면 0을 반환합니다.
    public static int calculateDays(Rental rental, LocalDate today) {
        if (!OVERDUE_STATUSES.contains(rental.getStatus()) || !today.isAfter(rental.getEndDate())) {
            return 0;
        }

        return Math.toIntExact(
                ChronoUnit.DAYS.between(rental.getEndDate(), today)
        );
    }
}
