package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.dto.response.PageResponse;
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
import com.example.iter.reservation.dto.response.RentalApproveResponse;
import com.example.iter.reservation.dto.response.RentalDetailResponse;
import com.example.iter.reservation.dto.response.RentalReceivedItemResponse;
import com.example.iter.reservation.dto.response.RentalRejectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        if (!equipment.isActive()) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (!startDate.isAfter(LocalDate.now())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }
        if (!startDate.isBefore(endDate)) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
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

        return RentalDetailResponse.of(rental, renter, owner, paymentStatus, overdueDays(rental));
    }

    @Transactional(readOnly = true)
    public PageResponse<RentalReceivedItemResponse> getReceivedRentals(Long ownerId, RentalStatus status,
                                                                         int page, int size) {
        Page<Rental> rentals = rentalRepository.findReceivedRentals(
                ownerId, status, PageRequest.of(page, size));

        Page<RentalReceivedItemResponse> response = rentals.map(rental -> {
            UserSummaryResponse renter = userRepository.findSummaryById(rental.getRenterId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            PaymentStatus paymentStatus = paymentRepository.findByRentalId(rental.getId())
                    .map(Payment::getStatus)
                    .orElse(null);
            return RentalReceivedItemResponse.of(rental, renter, paymentStatus);
        });

        return PageResponse.from(response);
    }

    @Transactional
    public RentalCancelResponse cancelRental(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalOrThrow(rentalId);

        if (!isAdmin && !rental.isRenter(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
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

    @Transactional
    public RentalApproveResponse approveRental(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalOrThrow(rentalId);
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!isAdmin && !equipment.isOwnedBy(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (rental.getStatus() != RentalStatus.REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_NOT_APPROVABLE);
        }

        equipment = equipmentRepository.findByIdForUpdate(equipment.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        // 1) 락을 잡은 상태에서 재검증 — 요청 이후 관리자가 장비를 중지/삭제시켰다면 승인 불가
        if (!equipment.isActive()) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
        }

        // 2) 이 사이 다른 트랜잭션이 먼저 커밋한 확정 예약이 있으면 승인 불가
        if (rentalRepository.existsConflictingConfirmedRental(
                equipment.getId(), rental.getStartDate(), rental.getEndDate())) {
            throw new CustomException(ErrorCode.RESERVATION_CONFLICT);
        }

        // 3) 충돌 없음 확인되면 예약 승인
        rental.approve();

        // 4) 같은 장비가 겹치는 기간의 다른 REQUESTED 예약들은 이 트랜잭션 안에서 자동 거절+환불 진행
        // - (별도 API 호출이 아니라, 승인 트랜잭션에 포함되어야 "1건만 확정"이 원자적으로 보장됨)
        List<Rental> competitors = rentalRepository.findOverlappingRequestedRentals(
                equipment.getId(), rental.getId(), rental.getStartDate(), rental.getEndDate());
        for (Rental competitor : competitors) {
            rejectAndRefund(competitor, "다른 예약이 먼저 승인되어 자동 거절되었습니다.");
        }

        return RentalApproveResponse.from(rental);
    }

    @Transactional
    public RentalRejectResponse rejectRental(Long rentalId, Long currentUserId, boolean isAdmin, String reason) {
        Rental rental = getRentalOrThrow(rentalId);
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!isAdmin && !equipment.isOwnedBy(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (rental.getStatus() != RentalStatus.REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_ALREADY_PROCESSED);
        }

        rejectAndRefund(rental, reason);

        PaymentStatus paymentStatus = paymentRepository.findByRentalId(rentalId)
                .map(Payment::getStatus)
                .orElse(null);
        return RentalRejectResponse.of(rental, paymentStatus);
    }

    // 결제 완료건이면 환불하고 예약을 REJECTED로 전환 — 수동 거절과 승인 시 경쟁 예약 자동거절에서 공유
    private void rejectAndRefund(Rental rental, String reason) {
        Payment payment = paymentRepository.findByRentalId(rental.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            userRepository.refundPointBalance(rental.getRenterId(), payment.getAmount());
            payment.markRefunded();
        }
        rental.reject(reason);
    }

    private Rental getRentalOrThrow(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    private int overdueDays(Rental rental) {
        if (!LocalDate.now().isAfter(rental.getEndDate()) || NOT_OVERDUE_ELIGIBLE.contains(rental.getStatus())) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(rental.getEndDate(), LocalDate.now());
    }
}
