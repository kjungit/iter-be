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
import com.example.iter.reservation.domain.entity.RentalReview;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.domain.repository.RentalReviewRepository;
import com.example.iter.reservation.event.RentalApprovedEvent;
import com.example.iter.reservation.event.RentalCanceledEvent;
import com.example.iter.reservation.event.RentalReceivedEvent;
import com.example.iter.reservation.event.RentalRejectedEvent;
import com.example.iter.reservation.event.RentalReviewCreatedEvent;
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
    private final RentalReviewRepository rentalReviewRepository;

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
            NotificationMessages.Content paymentCompletedOwner = NotificationMessages.paymentCompletedOwner(
                    renter != null ? renter.getName() : "대여자", productName, owner.getPreferredLanguage());
            notify(() -> notificationService.create(
                    owner.getId(), owner.getEmail(), NotificationType.PAYMENT_COMPLETED_OWNER,
                    paymentCompletedOwner.title(), paymentCompletedOwner.message(), paymentCompletedOwner.params(),
                    rental.getId()
            ));
            NotificationMessages.Content rentalRequested =
                    NotificationMessages.rentalRequested(productName, owner.getPreferredLanguage());
            notify(() -> notificationService.create(
                    owner.getId(), owner.getEmail(), NotificationType.RENTAL_REQUESTED,
                    rentalRequested.title(), rentalRequested.message(), rentalRequested.params(),
                    rental.getId()
            ));
        }
        if (renter != null) {
            NotificationMessages.Content paymentCompletedRenter =
                    NotificationMessages.paymentCompletedRenter(productName, renter.getPreferredLanguage());
            notify(() -> notificationService.create(
                    renter.getId(), renter.getEmail(), NotificationType.PAYMENT_COMPLETED_RENTER,
                    paymentCompletedRenter.title(), paymentCompletedRenter.message(), paymentCompletedRenter.params(),
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
        NotificationMessages.Content content = NotificationMessages.rentalApproved(
                rental.getProductNameSnapshot(), renter.getPreferredLanguage());
        notify(() -> notificationService.create(
                renter.getId(), renter.getEmail(), NotificationType.RENTAL_APPROVED,
                content.title(), content.message(), content.params(),
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

        NotificationMessages.Content content = NotificationMessages.rentalRejected(
                rental.getProductNameSnapshot(), rental.getRejectReason(), refunded, renter.getPreferredLanguage());
        notify(() -> notificationService.create(
                renter.getId(), renter.getEmail(), NotificationType.RENTAL_REJECTED,
                content.title(), content.message(), content.params(),
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

        NotificationMessages.Content content = NotificationMessages.rentalCanceled(
                renter != null ? renter.getName() : "대여자", rental.getProductNameSnapshot(),
                owner.getPreferredLanguage());
        notify(() -> notificationService.create(
                owner.getId(), owner.getEmail(), NotificationType.RENTAL_CANCELED,
                content.title(), content.message(), content.params(),
                rental.getId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalReceived(RentalReceivedEvent event) {
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

        NotificationMessages.Content content = NotificationMessages.rentalReceived(
                renter != null ? renter.getName() : "대여자", rental.getProductNameSnapshot(),
                owner.getPreferredLanguage());
        notify(() -> notificationService.create(
                owner.getId(), owner.getEmail(), NotificationType.RENTAL_RECEIVED,
                content.title(), content.message(), content.params(),
                rental.getId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRentalReviewCreated(RentalReviewCreatedEvent event) {
        RentalReview review = rentalReviewRepository.findById(event.reviewId()).orElse(null);
        if (review == null) {
            return;
        }
        Rental rental = rentalRepository.findById(review.getRentalId()).orElse(null);
        if (rental == null) {
            return;
        }
        User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
        User reviewee = userRepository.findById(review.getRevieweeId()).orElse(null);
        if (reviewee == null) {
            return;
        }

        NotificationMessages.Content content = NotificationMessages.reviewReceived(
                reviewer != null ? reviewer.getName() : "상대방",
                rental.getProductNameSnapshot(),
                review.getRating(),
                reviewee.getPreferredLanguage());
        notify(() -> notificationService.create(
                reviewee.getId(), reviewee.getEmail(), NotificationType.REVIEW_RECEIVED,
                content.title(), content.message(), content.params(),
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
