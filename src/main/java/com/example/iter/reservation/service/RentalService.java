package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.client.TossApiException;
import com.example.iter.payment.client.TossPaymentClient;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.service.model.RentalPaymentStatusRow;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.policy.RentalConflictPolicy;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import com.example.iter.reservation.dto.response.RentalCancelResponse;
import com.example.iter.reservation.dto.response.RentalCreateResponse;
import com.example.iter.reservation.dto.response.RentalApproveResponse;
import com.example.iter.reservation.dto.response.RentalDetailResponse;
import com.example.iter.reservation.dto.response.RentalReceivedItemResponse;
import com.example.iter.reservation.dto.response.RentalRejectResponse;
import com.example.iter.reservation.event.RentalApprovedEvent;
import com.example.iter.reservation.event.RentalCanceledEvent;
import com.example.iter.reservation.event.RentalRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
    private final TossPaymentClient tossPaymentClient;
    private final ApplicationEventPublisher eventPublisher;

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

        // 회원 탈퇴와 신규 대여 생성이 서로 같은 사용자 행 락에 참여하도록 한다.
        // 두 사용자를 항상 ID 오름차순으로 잠가 서로 상대방 장비를 동시에 대여할 때의 데드락도 줄인다.
        lockAndValidateRentalParticipants(renterId, equipment.getOwnerId());

        // 같은 장비에 대한 동시 요청을 직렬화하기 위해 락을 잡고 재조회 — 선점 방식이라
        // "겹치는지 확인"과 "저장"이 하나의 원자적 구간이어야 두 명이 동시에 같은 기간을 통과시키지 못한다.
        equipment = equipmentRepository.findByIdForUpdate(equipment.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!equipment.isActive()) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
        }

        if (!rentalRepository.findConflictingOccupyingRentalsForUpdate(
                equipment.getId(),
                startDate,
                endDate,
                RentalConflictPolicy.nonOccupyingStatuses()).isEmpty()) {
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
                .categorySnapshot(equipment.getCategory().name())
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
        log.info("대여 신청 처리: rentalId={}, equipmentId={}, renterId={}, status={}",
                savedRental.getId(), savedRental.getEquipmentId(), renterId, savedRental.getStatus());
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
                ownerId,
                status,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );

        Map<Long, UserSummaryResponse> renterMap = loadRenterSummaries(rentals.getContent());
        Map<Long, PaymentStatus> paymentStatusMap = loadPaymentStatuses(rentals.getContent());

        Page<RentalReceivedItemResponse> response = rentals.map(rental -> {
            UserSummaryResponse renter = renterMap.get(rental.getRenterId());
            if (renter == null) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            PaymentStatus paymentStatus = paymentStatusMap.get(rental.getId());
            return RentalReceivedItemResponse.of(rental, renter, paymentStatus);
        });

        return PageResponse.from(response);
    }

    private Map<Long, UserSummaryResponse> loadRenterSummaries(List<Rental> rentals) {
        if (rentals.isEmpty()) {
            return Map.of();
        }

        List<Long> renterIds = rentals.stream()
                .map(Rental::getRenterId)
                .distinct()
                .toList();

        return userRepository.findSummariesByIdIn(renterIds).stream()
                .collect(Collectors.toMap(UserSummaryResponse::userId, Function.identity()));
    }

    private Map<Long, PaymentStatus> loadPaymentStatuses(List<Rental> rentals) {
        if (rentals.isEmpty()) {
            return Map.of();
        }

        List<Long> rentalIds = rentals.stream()
                .map(Rental::getId)
                .toList();

        return paymentRepository.findStatusesByRentalIdIn(rentalIds).stream()
                .collect(Collectors.toMap(
                        RentalPaymentStatusRow::rentalId,
                        RentalPaymentStatusRow::paymentStatus
                ));
    }

    @Transactional
    public RentalCancelResponse cancelRental(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalWithLockOrThrow(rentalId);

        if (!isAdmin && !rental.isRenter(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_CANCEL_NOT_ALLOWED);
        }
        // owner는 결제 완료(REQUESTED) 시점에야 이 요청의 존재를 처음 알게 된다.
        // 아직 PENDING(결제 전)인 채로 취소되면 owner는 애초에 이 요청을 몰랐으므로,
        // "취소했습니다" 알림을 보내면 존재도 몰랐던 요청에 대한 뜬금없는 알림이 된다.
        boolean ownerWasNotified = rental.getStatus() == RentalStatus.REQUESTED;

        Payment payment = paymentRepository.findByRentalId(rentalId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            cancelTossPayment(payment, "대여 취소");
        }

        rental.changeStatus(RentalStatus.CANCELED);
        if (ownerWasNotified) {
            eventPublisher.publishEvent(new RentalCanceledEvent(rental.getId()));
        }

        PaymentStatus paymentStatus = payment != null? payment.getStatus(): null;
        log.info("대여 취소 처리: rentalId={}, actorId={}, actorType={}, status={}, paymentStatus={}",
                rentalId, currentUserId, isAdmin ? "ADMIN" : "USER", rental.getStatus(), paymentStatus);

        return RentalCancelResponse.of(rental, paymentStatus);
    }

    @Transactional
    public RentalApproveResponse approveRental(Long rentalId, Long currentUserId, boolean isAdmin) {
        Rental rental = getRentalWithLockOrThrow(rentalId);
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
        if (!rentalRepository.findConflictingOccupyingRentalsForUpdate(
                equipment.getId(),
                rental.getStartDate(),
                rental.getEndDate(),
                RentalConflictPolicy.nonConfirmedStatuses()).isEmpty()) {
            throw new CustomException(ErrorCode.RESERVATION_CONFLICT);
        }

        // 3) 충돌 없음 확인되면 예약 승인
        // 선점 방식(createRental 시점 락)이라 같은 기간에 REQUESTED가 동시에 여러 건 존재할 수 없어서
        // 예전처럼 "겹치는 다른 REQUESTED 자동 거절" 로직은 더 이상 필요 없다.
        rental.approve();
        eventPublisher.publishEvent(new RentalApprovedEvent(rental.getId()));
        log.info("대여 승인 처리: rentalId={}, equipmentId={}, actorId={}, actorType={}, status={}",
                rentalId, equipment.getId(), currentUserId, isAdmin ? "ADMIN" : "USER", rental.getStatus());

        return RentalApproveResponse.from(rental);
    }

    @Transactional
    public RentalRejectResponse rejectRental(Long rentalId, Long currentUserId, boolean isAdmin, String reason) {
        Rental rental = getRentalWithLockOrThrow(rentalId);
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!isAdmin && !equipment.isOwnedBy(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (rental.getStatus() != RentalStatus.REQUESTED) {
            throw new CustomException(ErrorCode.RENTAL_ALREADY_PROCESSED);
        }

        rejectAndRefund(rental, reason);
        eventPublisher.publishEvent(new RentalRejectedEvent(rental.getId()));

        PaymentStatus paymentStatus = paymentRepository.findByRentalId(rentalId)
                .map(Payment::getStatus)
                .orElse(null);
        log.info("대여 거절 처리: rentalId={}, actorId={}, actorType={}, status={}, paymentStatus={}",
                rentalId, currentUserId, isAdmin ? "ADMIN" : "USER", rental.getStatus(), paymentStatus);
        return RentalRejectResponse.of(rental, paymentStatus);
    }

    // 30분 안에 결제(confirm)를 완료하지 않은 PENDING 요청을 자동 취소해 선점을 풀어준다.
    // RentalExpirationScheduler가 주기적으로 호출한다.
    @Transactional
    public int expirePendingRentals() {
        return rentalRepository.expirePendingRentals(LocalDateTime.now().minusMinutes(30));
    }

    // 결제 완료건이면 환불하고 예약을 REJECTED로 전환
    private void rejectAndRefund(Rental rental, String reason) {
        Payment payment = paymentRepository.findByRentalId(rental.getId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            cancelTossPayment(payment, reason);
        }
        rental.reject(reason);
    }

    // 실제 토스 결제 취소 요청 — 실패하면 예약도 취소/거절 처리하지 않도록 예외를 그대로 전파한다
    // (토스 취소가 안 됐는데 우리 쪽만 취소 처리하면 돈과 상태가 어긋난다).
    private void cancelTossPayment(Payment payment, String reason) {
        try {
            tossPaymentClient.cancel(payment.getPaymentKey(), reason, payment.ensureCancelIdempotencyKey());
            payment.markRefunded();
        } catch (TossApiException e) {
            throw new CustomException(ErrorCode.TOSS_PAYMENT_FAILED);
        }
    }

    private Rental getRentalOrThrow(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    // 같은 거래에 대한 승인/취소/거절이 동시에 들어와도 순서대로 처리되도록 잠가서 조회한다.
    // 락 없이 조회하면 두 트랜잭션이 같은 버전을 읽고 나중에 커밋하는 쪽에서 낙관적 락 예외가 터진다
    // (부하테스트에서 확인된 문제 — GlobalExceptionHandler의 낙관적 락 핸들러는 그래도 발생할 수 있는
    // 다른 경합에 대비한 방어선이고, 이 락이 같은 rentalId에 대한 경합 자체를 직렬화하는 1차 방지책).
    private Rental getRentalWithLockOrThrow(Long rentalId) {
        return rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    private void lockAndValidateRentalParticipants(Long renterId, Long ownerId) {
        Long firstId = Math.min(renterId, ownerId);
        Long secondId = Math.max(renterId, ownerId);
        User first = findUserWithLock(firstId);
        User second = findUserWithLock(secondId);
        User renter = first.getId().equals(renterId) ? first : second;
        User owner = first.getId().equals(ownerId) ? first : second;

        if (renter.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        if (renter.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
        if (!owner.isActive()) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
        }
    }

    private User findUserWithLock(Long userId) {
        return userRepository.findWithLockById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private int overdueDays(Rental rental) {
        if (!LocalDate.now().isAfter(rental.getEndDate()) || NOT_OVERDUE_ELIGIBLE.contains(rental.getStatus())) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(rental.getEndDate(), LocalDate.now());
    }
}
