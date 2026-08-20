package com.example.iter.notification.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.notification.domain.entity.NotificationType;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.event.PaymentConfirmedEvent;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.event.RentalApprovedEvent;
import com.example.iter.reservation.event.RentalCanceledEvent;
import com.example.iter.reservation.event.RentalRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Rental/Payment 트랜잭션이 "커밋된 이후"에만 반응한다 (phase = AFTER_COMMIT).
// 예를 들어 approveRental()이 재고 충돌로 롤백되면 알림도 나가면 안 되기 때문.
// 알림 생성 하나가 실패해도(예: DB 커넥션 순간 장애) 같은 이벤트로 보낼 나머지 알림까지 막히면 안 되므로
// 각 알림 생성을 개별적으로 try-catch해서 격리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        Rental rental = rentalRepository.findById(event.rentalId()).orElse(null);
        if (rental == null) {
            return;
        }
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId()).orElse(null);
        if (equipment == null) {
            return;
        }
        User renter = userRepository.findById(rental.getRenterId()).orElse(null);
        User owner = userRepository.findById(equipment.getOwnerId()).orElse(null);

        String productName = rental.getProductNameSnapshot();

        if (owner != null) {
            notify(() -> notificationService.create(
                    owner.getId(), owner.getEmail(), NotificationType.PAYMENT_COMPLETED_OWNER,
                    "결제가 완료되었습니다",
                    "%s님이 [%s] 대여 건의 결제를 완료했습니다.".formatted(
                            renter != null ? renter.getName() : "대여자", productName),
                    rental.getId()
            ));
            notify(() -> notificationService.create(
                    owner.getId(), owner.getEmail(), NotificationType.RENTAL_REQUESTED,
                    "새로운 대여 신청이 도착했습니다",
                    "[%s] 대여 신청이 도착했습니다. 승인 대기 목록을 확인해주세요.".formatted(productName),
                    rental.getId()
            ));
        }
        if (renter != null) {
            notify(() -> notificationService.create(
                    renter.getId(), renter.getEmail(), NotificationType.PAYMENT_COMPLETED_RENTER,
                    "결제가 완료되었습니다",
                    "[%s] 대여 결제가 완료되었습니다. 등록자의 승인을 기다려주세요.".formatted(productName),
                    rental.getId()
            ));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalApproved(RentalApprovedEvent event) {
        Rental rental = rentalRepository.findById(event.rentalId()).orElse(null);
        if (rental == null) {
            return;
        }
        User renter = userRepository.findById(rental.getRenterId()).orElse(null);
        if (renter == null) {
            return;
        }
        notify(() -> notificationService.create(
                renter.getId(), renter.getEmail(), NotificationType.RENTAL_APPROVED,
                "대여 요청이 승인되었습니다",
                "[%s] 대여 요청이 승인되었습니다.".formatted(rental.getProductNameSnapshot()),
                rental.getId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalRejected(RentalRejectedEvent event) {
        Rental rental = rentalRepository.findById(event.rentalId()).orElse(null);
        if (rental == null) {
            return;
        }
        User renter = userRepository.findById(rental.getRenterId()).orElse(null);
        if (renter == null) {
            return;
        }

        boolean refunded = paymentRepository.findByRentalId(rental.getId())
                .map(Payment::getStatus)
                .map(status -> status == PaymentStatus.REFUNDED)
                .orElse(false);
        String refundNotice = refunded ? " 결제 금액은 환불 처리되었습니다." : "";

        notify(() -> notificationService.create(
                renter.getId(), renter.getEmail(), NotificationType.RENTAL_REJECTED,
                "대여 요청이 거절되었습니다",
                "[%s] 대여 요청이 거절되었습니다. 사유: %s.%s".formatted(
                        rental.getProductNameSnapshot(), rental.getRejectReason(), refundNotice),
                rental.getId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalCanceled(RentalCanceledEvent event) {
        Rental rental = rentalRepository.findById(event.rentalId()).orElse(null);
        if (rental == null) {
            return;
        }
        Equipment equipment = equipmentRepository.findById(rental.getEquipmentId()).orElse(null);
        if (equipment == null) {
            return;
        }
        User owner = userRepository.findById(equipment.getOwnerId()).orElse(null);
        User renter = userRepository.findById(rental.getRenterId()).orElse(null);
        if (owner == null) {
            return;
        }

        notify(() -> notificationService.create(
                owner.getId(), owner.getEmail(), NotificationType.RENTAL_CANCELED,
                "대여 요청이 취소되었습니다",
                "%s님이 [%s] 대여 요청을 취소했습니다.".formatted(
                        renter != null ? renter.getName() : "대여자", rental.getProductNameSnapshot()),
                rental.getId()
        ));
    }

    private void notify(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("알림 생성 실패", e);
        }
    }
}
