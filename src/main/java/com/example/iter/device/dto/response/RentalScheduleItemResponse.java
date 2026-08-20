package com.example.iter.device.dto.response;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;

import java.time.LocalDate;

public record RentalScheduleItemResponse(
        Long rentalId,
        LocalDate startDate,
        LocalDate endDate,
        RentalStatus status
) {
    public static RentalScheduleItemResponse from(Rental rental) {
        return new RentalScheduleItemResponse(
                rental.getId(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getStatus()
        );
    }
}
