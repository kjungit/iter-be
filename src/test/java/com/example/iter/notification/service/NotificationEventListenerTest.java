package com.example.iter.notification.service;

import com.example.iter.auth.domain.entity.PreferredLanguage;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.notification.domain.entity.NotificationType;
import com.example.iter.payment.domain.entity.Payment;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.event.PaymentConfirmedEvent;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.event.RentalApprovedEvent;
import com.example.iter.reservation.event.RentalCanceledEvent;
import com.example.iter.reservation.event.RentalReceivedEvent;
import com.example.iter.reservation.event.RentalRejectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// AFTER_COMMIT 리스너 자체(트랜잭션 경계)는 스프링이 보장하는 영역이라 검증하지 않고,
// "이벤트가 들어오면 어떤 알림을 누구에게 만드는가"라는 이 리스너 고유의 로직만 검증한다.
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private NotificationEventListener listener;

    private Rental rental(RentalStatus status) {
        return Rental.builder()
                .id(10L)
                .equipmentId(1L)
                .renterId(2L)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 25))
                .productNameSnapshot("소니 A7C2")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(5)
                .totalPrice(BigDecimal.valueOf(150000))
                .status(status)
                .rejectReason("일정이 겹칩니다.")
                .build();
    }

    private Equipment equipment() {
        return Equipment.builder()
                .id(1L)
                .ownerId(99L)
                .category(EquipmentCategory.CAMERA)
                .name("소니 A7C2")
                .dailyPrice(BigDecimal.valueOf(30000))
                .build();
    }

    private User user(Long id, String email, String name) {
        return User.builder().id(id).email(email).name(name).build();
    }

    @Test
    void 결제_확인_이벤트는_owner에게_2건_renter에게_1건_알림을_만든다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, "owner@test.com", "등록자")));

        listener.onPaymentConfirmed(new PaymentConfirmedEvent(10L));

        verify(notificationService).create(
                eq(99L), eq("owner@test.com"), eq(NotificationType.PAYMENT_COMPLETED_OWNER),
                any(), any(), any(), eq(10L));
        verify(notificationService).create(
                eq(99L), eq("owner@test.com"), eq(NotificationType.RENTAL_REQUESTED),
                any(), any(), any(), eq(10L));
        verify(notificationService).create(
                eq(2L), eq("renter@test.com"), eq(NotificationType.PAYMENT_COMPLETED_RENTER),
                any(), any(), any(), eq(10L));
    }

    @Test
    void 대여_정보가_없으면_결제_확인_이벤트를_무시한다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.empty());

        listener.onPaymentConfirmed(new PaymentConfirmedEvent(10L));

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 알림_하나가_실패해도_같은_이벤트의_나머지_알림은_계속_생성된다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, "owner@test.com", "등록자")));
        doThrow(new RuntimeException("일시적 DB 오류"))
                .when(notificationService)
                .create(eq(99L), any(), eq(NotificationType.PAYMENT_COMPLETED_OWNER), any(), any(), any(), anyLong());

        listener.onPaymentConfirmed(new PaymentConfirmedEvent(10L));

        verify(notificationService, times(3)).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 승인_이벤트는_renter에게_알림을_만든다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.APPROVED)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));

        listener.onRentalApproved(new RentalApprovedEvent(10L));

        verify(notificationService).create(
                eq(2L), eq("renter@test.com"), eq(NotificationType.RENTAL_APPROVED),
                any(), any(), any(), eq(10L));
    }

    @Test
    void 영어를_선호하는_수신자에게는_영어_이메일_문구를_만든다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.APPROVED)));
        User englishRenter = User.builder()
                .id(2L)
                .email("renter@test.com")
                .name("대여자")
                .preferredLanguage(PreferredLanguage.EN)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(englishRenter));

        listener.onRentalApproved(new RentalApprovedEvent(10L));

        verify(notificationService).create(
                eq(2L), eq("renter@test.com"), eq(NotificationType.RENTAL_APPROVED),
                eq("Rental request approved"), startsWith("Your rental request for"), any(), eq(10L));
    }

    @Test
    void 환불된_거절이면_메시지에_환불_안내가_포함된다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.REJECTED)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));
        Payment refunded = Payment.builder().rentalId(10L).amount(BigDecimal.valueOf(150000)).build();
        refunded.markRefunded();
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.of(refunded));

        listener.onRentalRejected(new RentalRejectedEvent(10L));

        verify(notificationService).create(
                eq(2L), eq("renter@test.com"), eq(NotificationType.RENTAL_REJECTED),
                any(), contains("환불"), any(), eq(10L));
    }

    @Test
    void 환불이_없는_거절이면_메시지에_환불_안내가_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.REJECTED)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.empty());

        listener.onRentalRejected(new RentalRejectedEvent(10L));

        verify(notificationService).create(
                eq(2L), eq("renter@test.com"), eq(NotificationType.RENTAL_REJECTED),
                any(), argThat((String message) -> !message.contains("환불")), any(), eq(10L));
    }

    @Test
    void 취소_이벤트는_owner에게_알림을_만든다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.CANCELED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, "owner@test.com", "등록자")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));

        listener.onRentalCanceled(new RentalCanceledEvent(10L));

        verify(notificationService).create(
                eq(99L), eq("owner@test.com"), eq(NotificationType.RENTAL_CANCELED),
                any(), any(), any(), eq(10L));
    }

    @Test
    void 수령확인_이벤트는_owner에게_알림을_만든다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.RENTING)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment()));
        when(userRepository.findById(99L)).thenReturn(Optional.of(user(99L, "owner@test.com", "등록자")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "renter@test.com", "대여자")));

        listener.onRentalReceived(new RentalReceivedEvent(10L));

        verify(notificationService).create(
                eq(99L), eq("owner@test.com"), eq(NotificationType.RENTAL_RECEIVED),
                any(), any(), any(), eq(10L));
    }

    @Test
    void 장비_정보가_없으면_수령확인_이벤트를_무시한다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(RentalStatus.RENTING)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        listener.onRentalReceived(new RentalReceivedEvent(10L));

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }
}
