package com.example.iter.reservation.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.domain.repository.PaymentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @InjectMocks
    private RentalService rentalService;

    private Equipment equipment(Long ownerId) {
        return equipment(ownerId, EquipmentStatus.ACTIVE);
    }

    private Equipment equipment(Long ownerId, EquipmentStatus status) {
        return Equipment.builder()
                .id(1L)
                .ownerId(ownerId)
                .category("카메라")
                .name("소니 A7C2")
                .dailyPrice(BigDecimal.valueOf(30000))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build();
    }

    private RentalCreateRequest request() {
        return new RentalCreateRequest(1L, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 25),
                "홍길동", "010-0000-0000", "12345", "서울시", "101동", "문 앞", true);
    }

    private Rental rental(Long id, RentalStatus status) {
        return Rental.builder()
                .id(id)
                .equipmentId(1L)
                .renterId(2L)
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
    void 대여_요청_생성시_일수와_총액을_계산한다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingActiveRental(anyLong(), any(), any())).thenReturn(false);
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
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingActiveRental(anyLong(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_PERIOD_CONFLICT);
    }

    @Test
    void 장비_소유자가_아니면_승인할_수_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 2L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void REQUESTED_상태가_아니면_승인할_수_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.APPROVED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.approveRental(10L, 99L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_NOT_APPROVABLE);
    }

    @Test
    void 승인_시점에_장비가_비활성_상태면_승인할_수_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
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
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingConfirmedRental(anyLong(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> rentalService.approveRental(10L, 99L, false))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVATION_CONFLICT);
    }

    @Test
    void 충돌이_없으면_승인된다() {
        Rental target = rental(10L, RentalStatus.REQUESTED);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(target));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(equipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingConfirmedRental(anyLong(), any(), any())).thenReturn(false);

        var response = rentalService.approveRental(10L, 99L, false);

        assertThat(response.status()).isEqualTo(RentalStatus.APPROVED);
    }

    @Test
    void 권한이_없으면_거절할_수_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REQUESTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.rejectRental(10L, 2L, false, "사유"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 이미_처리된_요청은_다시_거절할_수_없다() {
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.REJECTED)));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        assertThatThrownBy(() -> rentalService.rejectRental(10L, 99L, false, "사유"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ALREADY_PROCESSED);
    }

    @Test
    void 정상_거절시_상태와_사유가_저장된다() {
        Rental target = rental(10L, RentalStatus.REQUESTED);
        when(rentalRepository.findById(10L)).thenReturn(Optional.of(target));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));

        var response = rentalService.rejectRental(10L, 99L, false, "일정이 겹칩니다.");

        assertThat(response.status()).isEqualTo(RentalStatus.REJECTED);
        assertThat(response.reason()).isEqualTo("일정이 겹칩니다.");
    }
}
