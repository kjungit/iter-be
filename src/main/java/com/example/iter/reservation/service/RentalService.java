package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import com.example.iter.reservation.dto.response.RentalCancelResponse;
import com.example.iter.reservation.dto.response.RentalCreateResponse;
import com.example.iter.reservation.dto.response.RentalDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RentalService {

    private static final Set<RentalStatus> NOT_OVERDUE_ELIGIBLE = Set.of(
            RentalStatus.COMPLETED, RentalStatus.CANCELED, RentalStatus.REJECTED, RentalStatus.DISPUTED
    );

    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public RentalCreateResponse createRental(Long renterId, RentalCreateRequest request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (equipment.isOwnedBy(renterId)) {
            throw new CustomException(ErrorCode.EQUIPMENT_SELF_RENTAL);
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        if (!startDate.isBefore(endDate)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (rentalRepository.existsConflictingConfirmedRental(equipment.getId(), startDate, endDate)) {
            throw new CustomException(ErrorCode.RENTAL_PERIOD_CONFLICT);
        }

        int rentalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal totalPrice = equipment.getDailyPrice().multiply(BigDecimal.valueOf(rentalDays));

        Rental rental = Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(renterId)
                .startDate(startDate)
                .endDate(endDate)
                .productNameSnapshot(equipment.getName())
                .categorySnapshot(equipment.getCategory())
                .dailyPriceSnapshot(equipment.getDailyPrice())
                .rentalDays(rentalDays)
                .totalPrice(totalPrice)
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .zipcode(request.zipcode())
                .address(request.address())
                .detailAddress(request.detailAddress())
                .requestMessage(request.requestMessage())
                .build();

        Rental savedRental = rentalRepository.save(rental);
        return RentalCreateResponse.from(savedRental);
    }

    @Transactional(readOnly = true)
    public RentalDetailResponse getRentalDetail(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalOrThrow(rentalId);
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        boolean isParty = rental.isRenter(currentUserId) || equipment.isOwnedBy(currentUserId);
        if (!isParty && !isAdmin) {
            throw new CustomException(ErrorCode.RENTAL_NOT_PARTY);
        }

        User renter = userRepository.findById(rental.getRenterId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User owner = userRepository.findById(equipment.getOwnerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        PaymentStatus paymentStatus = paymentRepository.findByRentalId(rentalId)
                .map(Payment::getStatus)
                .orElse(null);

        return RentalDetailResponse.of(rental, equipment, renter, owner, paymentStatus, isOverdue(rental));
    }

    @Transactional
    public RentalCancelResponse cancelRental(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalOrThrow(rentalId);

        if (!isAdmin && !rental.isRenter(currentUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_CANCEL_NOT_ALLOWED);
        }

        Payment payment = paymentRepository.findByRentalId(rentalId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            userRepository.refundPointBalance(rental.getRenterId(), payment.getAmount());
            payment.markRefunded();
        }

        rental.changeStatus(RentalStatus.CANCELED);

        BigDecimal pointBalance = userRepository.findById(rental.getRenterId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getPointBalance();

        PaymentStatus paymentStatus = payment != null? payment.getStatus(): null;

        return RentalCancelResponse.of(rental, paymentStatus, pointBalance);
    }

    private Rental getRentalOrThrow(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    private boolean isOverdue(Rental rental) {
        return LocalDate.now().isAfter(rental.getEndDate()) && !NOT_OVERDUE_ELIGIBLE.contains(rental.getStatus());
    }
}
