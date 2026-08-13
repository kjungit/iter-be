package com.example.iter.auth.service;

import com.example.iter.reservation.domain.entity.RentalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<RentalStatus> ESTABLISHED_STATUSES = EnumSet.of(
            RentalStatus.APPROVED,
            RentalStatus.SHIPPING,
            RentalStatus.RECEIVED,
            RentalStatus.RENTING,
            RentalStatus.RETURN_REQUESTED,
            RentalStatus.RETURNING,
            RentalStatus.RETURNED,
            RentalStatus.DISPUTED,
            RentalStatus.COMPLETED
    );

    private static final Set<RentalStatus> OVERDUE_STATUSES = EnumSet.of(
            RentalStatus.RECEIVED,
            RentalStatus.RENTING,
            RentalStatus.RETURN_REQUESTED,
            RentalStatus.RETURNING
    );
}
