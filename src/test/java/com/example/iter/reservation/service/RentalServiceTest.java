package com.example.iter.reservation.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.payment.domain.repository.PaymentRepository;
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

    @Test
    void 대여_요청_생성시_일수와_총액을_계산한다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingConfirmedRental(anyLong(), any(), any())).thenReturn(false);
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
    void 겹치는_확정_예약이_있으면_요청할_수_없다() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment(99L)));
        when(rentalRepository.existsConflictingConfirmedRental(anyLong(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> rentalService.createRental(2L, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_PERIOD_CONFLICT);
    }
}
