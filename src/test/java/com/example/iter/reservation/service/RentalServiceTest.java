package com.example.iter.reservation.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.dto.response.UserSummaryResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.payment.domain.entity.PaymentStatus;
import com.example.iter.payment.service.model.RentalPaymentStatusRow;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import com.example.iter.reservation.event.RentalApprovedEvent;
import com.example.iter.reservation.event.RentalCanceledEvent;
import com.example.iter.reservation.event.RentalRejectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RentalService rentalService;

    private Equipment equipment(Long ownerId) {
        return equipment(ownerId, EquipmentStatus.ACTIVE);
    }

    private Equipment equipment(Long ownerId, EquipmentStatus status) {
        return Equipment.builder()
                .id(1L)
                .ownerId(ownerId)
                .category(EquipmentCategory.CAMERA)
                .name("소니 A7C2")
                .dailyPrice(BigDecimal.valueOf(30000))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build();
    }

    private RentalCreateRequest request() {
        return new RentalCreateRequest(1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(10),
                "홍길동", "010-0000-0000", "12345", "서울시", "101동", "문 앞", true);
    }

    private Rental rental(Long id, RentalStatus status) {
        return rental(id, 2L, status);
    }

    private Rental rental(Long id, Long renterId, RentalStatus status) {
        return Rental.builder()
                .id(id)
                .equipmentId(1L)
                .renterId(renterId)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 25))
                .productNameSnapshot("소니 A7C2")
                .categorySnapshot("카메라")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(6)
                .totalPrice(BigDecimal.valueOf(180000))
                .status(status)
                .build();
    }

    @Test
    void 받은_대여_요청은_회원과_결제_상태를_각각_한번에_조회한다() {
        Rental first = rental(10L, 2L, RentalStatus.REQUESTED);
        Rental second = rental(11L, 3L, RentalStatus.REQUESTED);
        PageRequest expectedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        when(rentalRepository.findReceivedRentals(99L, null, expectedPageable))
                .thenReturn(new PageImpl<>(List.of(first, second), expectedPageable, 2));
        when(userRepository.findSummariesByIdIn(List.of(2L, 3L)))
                .thenReturn(List.of(
                        new UserSummaryResponse(2L, "대여자2"),
                        new UserSummaryResponse(3L, "대여자3")
                ));
        when(paymentRepository.findStatusesByRentalIdIn(List.of(10L, 11L)))
                .thenReturn(List.of(new RentalPaymentStatusRow(10L, PaymentStatus.PAID)));

        var response = rentalService.getReceivedRentals(99L, null, 0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).renter().nickName()).isEqualTo("대여자2");
        assertThat(response.content().get(0).paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.content().get(1).renter().nickName()).isEqualTo("대여자3");
        assertThat(response.content().get(1).paymentStatus()).isNull();
        assertThat(response.totalElements()).isEqualTo(2);
        verify(userRepository).findSummariesByIdIn(List.of(2L, 3L));
        verify(paymentRepository).findStatusesByRentalIdIn(List.of(10L, 11L));
        verify(userRepository, never()).findSummaryById(anyLong());
        verify(paymentRepository, never()).findByRentalId(11L);
    }

    @Test
    void 받은_대여_요청의_대여자_정보가_없으면_예외를_던진다() {
        Rental target = rental(10L, 2L, RentalStatus.REQUESTED);
        when(rentalRepository.findReceivedRentals(anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(target)));
        when(userRepository.findSummariesByIdIn(List.of(2L))).thenReturn(List.of());
        when(paymentRepository.findStatusesByRentalIdIn(List.of(10L))).thenReturn(List.of());

        assertThatThrownBy(() -> rentalService.getReceivedRentals(99L, null, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 받은_대여_요청이_비어있으면_일괄_조회하지_않는다() {
        when(rentalRepository.findReceivedRentals(anyLong(), any(), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        var response = rentalService.getReceivedRentals(99L, null, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(userRepository, never()).findSummariesByIdIn(any());
        verify(paymentRepository, never()).findStatusesByRentalIdIn(any());
    }

    @Test
    void 대여_요청_생성시_일수와_총액을_계산한다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        mockParticipants(UserStatus.ACTIVE, UserStatus.ACTIVE);
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.findConflictingOccupyingRentalsForUpdate(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        when(rentalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = rentalService.createRental(2L, request());

        assertThat(response.rentalDays()).isEqualTo(6);
        assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(180000));
    }

    @Test
    void 본인_장비는_대여할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(2L)));

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_SELF_RENTAL);
    }

    @Test
    void ACTIVE_상태가_아닌_장비는_대여할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L, EquipmentStatus.MAINTENANCE)));

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
    }

    @Test
    void 시작일이_오늘이거나_과거면_요청할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        RentalCreateRequest todayRequest = new RentalCreateRequest(1L,
                LocalDate.now(), LocalDate.now().plusDays(5),
                "홍길동", "010-0000-0000", "12345", "서울시", "101동", "문 앞", true);

        assertThatThrownBy(() -> rentalService.createRental(2L, todayRequest))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 겹치는_예약이_있으면_요청할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        mockParticipants(UserStatus.ACTIVE, UserStatus.ACTIVE);
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.findConflictingOccupyingRentalsForUpdate(anyLong(), any(), any(), any()))
                .thenReturn(List.of(rental(20L, RentalStatus.RENTING)));

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_PERIOD_CONFLICT);
    }

    @Test
    void 정지된_회원은_새로운_대여를_요청할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        mockParticipants(UserStatus.SUSPENDED, UserStatus.ACTIVE);

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    @Test
    void 탈퇴한_소유자의_장비에는_새로운_대여를_요청할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        mockParticipants(UserStatus.ACTIVE, UserStatus.DELETED);

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
    }

    @Test
    void 장비_소유자가_아니면_승인할_수_없다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 2L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void REQUESTED_상태가_아니면_승인할_수_없다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.APPROVED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 99L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_NOT_APPROVABLE);
    }

    @Test
    void 승인_시점에_장비가_비활성_상태면_승인할_수_없다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(equipment(99L, EquipmentStatus.SUSPENDED)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 99L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
    }

    @Test
    void 이미_확정된_예약과_겹치면_RESERVATION_CONFLICT를_던진다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.findConflictingOccupyingRentalsForUpdate(anyLong(), any(), any(), any()))
                .thenReturn(List.of(rental(20L, RentalStatus.RENTING)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 99L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVATION_CONFLICT);
    }

    private void mockParticipants(UserStatus renterStatus, UserStatus ownerStatus) {
        when(userRepository.findWithLockById(2L))
                .thenReturn(Optional.of(user(2L, renterStatus)));
        when(userRepository.findWithLockById(99L))
                .thenReturn(Optional.of(user(99L, ownerStatus)));
    }

    private User user(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .email("user-" + id + "@example.com")
                .name("테스트 회원")
                .status(status)
                .build();
    }

    @Test
    void 충돌이_없으면_승인된다() {
        Rental target = rental(10L, RentalStatus.REQUESTED);
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(target));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.findConflictingOccupyingRentalsForUpdate(anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        var response = rentalService.approveRental(10L, 99L, false);

        assertThat(response.status()).isEqualTo(RentalStatus.APPROVED);
        verify(eventPublisher).publishEvent(new RentalApprovedEvent(10L));
    }

    @Test
    void 권한이_없으면_거절할_수_없다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.rejectRental(10L, 2L, false, "사유"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 이미_처리된_요청은_다시_거절할_수_없다() {
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REJECTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.rejectRental(10L, 99L, false, "사유"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ALREADY_PROCESSED);
    }

    @Test
    void 정상_거절시_상태와_사유가_저장된다() {
        Rental target = rental(10L, RentalStatus.REQUESTED);
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(target));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        var response = rentalService.rejectRental(10L, 99L, false, "일정이 겹칩니다.");

        assertThat(response.status()).isEqualTo(RentalStatus.REJECTED);
        assertThat(response.reason()).isEqualTo("일정이 겹칩니다.");
        verify(eventPublisher).publishEvent(new RentalRejectedEvent(10L));
    }

    @Test
    void REQUESTED_상태에서_취소하면_취소_이벤트를_발행한다() {
        // owner는 REQUESTED(결제 완료) 시점에야 이 요청을 처음 알게 되므로,
        // 그 이후 취소는 owner가 이미 아는 요청에 대한 취소라 알림 대상이다.
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.empty());

        var response = rentalService.cancelRental(10L, 2L, false);

        assertThat(response.status()).isEqualTo(RentalStatus.CANCELED);
        verify(eventPublisher).publishEvent(new RentalCanceledEvent(10L));
    }

    @Test
    void PENDING_상태에서_취소하면_취소_이벤트를_발행하지_않는다() {
        // owner는 결제 전(PENDING) 요청의 존재를 아직 모르므로, 취소 알림을 보내면 안 된다.
        when(rentalRepository.findWithLockById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.PENDING)));
        when(paymentRepository.findByRentalId(10L)).thenReturn(Optional.empty());

        var response = rentalService.cancelRental(10L, 2L, false);

        assertThat(response.status()).isEqualTo(RentalStatus.CANCELED);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
