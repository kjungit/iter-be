package com.example.iter.dispute.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportTargetValidatorTest {

    private static final Long REPORTER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private ReportTargetValidator reportTargetValidator;

    @Test
    void 자기_자신은_회원_신고할_수_없다() {
        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.USER,
                REPORTER_ID,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_SELF_TARGET_NOT_ALLOWED);

        verifyNoInteractions(userRepository);
    }

    @Test
    void 존재하지_않거나_탈퇴한_회원은_신고할_수_없다() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.USER,
                2L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, UserStatus.DELETED)));

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.USER,
                3L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 정지된_회원은_신고_대상으로_선택할_수_있다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserStatus.SUSPENDED)));

        assertThatCode(() -> reportTargetValidator.validate(
                ReportTargetType.USER,
                2L,
                REPORTER_ID
        )).doesNotThrowAnyException();
    }

    @Test
    void 본인_소유_장비는_신고할_수_없다() {
        when(equipmentRepository.findById(10L))
                .thenReturn(Optional.of(equipment(10L, REPORTER_ID, EquipmentStatus.ACTIVE)));

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.EQUIPMENT,
                10L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_SELF_TARGET_NOT_ALLOWED);
    }

    @Test
    void 삭제된_장비는_신고할_수_없다() {
        when(equipmentRepository.findById(10L))
                .thenReturn(Optional.of(equipment(10L, 2L, EquipmentStatus.DELETED)));

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.EQUIPMENT,
                10L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_FOUND);
    }

    @Test
    void 타인_소유의_점검중인_장비도_신고할_수_있다() {
        when(equipmentRepository.findById(10L))
                .thenReturn(Optional.of(equipment(10L, 2L, EquipmentStatus.MAINTENANCE)));

        assertThatCode(() -> reportTargetValidator.validate(
                ReportTargetType.EQUIPMENT,
                10L,
                REPORTER_ID
        )).doesNotThrowAnyException();
    }

    @Test
    void 거래의_대여자와_장비_등록자는_거래를_신고할_수_있다() {
        Rental renterRental = rental(100L, 10L, REPORTER_ID);
        Equipment ownerEquipment = equipment(10L, 2L, EquipmentStatus.DELETED);
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(renterRental));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(ownerEquipment));

        assertThatCode(() -> reportTargetValidator.validate(
                ReportTargetType.RENTAL,
                100L,
                REPORTER_ID
        )).doesNotThrowAnyException();

        Rental ownerRental = rental(101L, 11L, 3L);
        Equipment reporterEquipment = equipment(11L, REPORTER_ID, EquipmentStatus.ACTIVE);
        when(rentalRepository.findById(101L)).thenReturn(Optional.of(ownerRental));
        when(equipmentRepository.findById(11L)).thenReturn(Optional.of(reporterEquipment));

        assertThatCode(() -> reportTargetValidator.validate(
                ReportTargetType.RENTAL,
                101L,
                REPORTER_ID
        )).doesNotThrowAnyException();
    }

    @Test
    void 거래_제3자는_거래를_신고할_수_없다() {
        Rental rental = rental(100L, 10L, 2L);
        Equipment equipment = equipment(10L, 3L, EquipmentStatus.ACTIVE);
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment));

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.RENTAL,
                100L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_NOT_PARTY);
    }

    @Test
    void 존재하지_않는_거래는_신고할_수_없다() {
        when(rentalRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportTargetValidator.validate(
                ReportTargetType.RENTAL,
                100L,
                REPORTER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_NOT_FOUND);

        verify(equipmentRepository, never()).findById(10L);
    }

    private User user(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .email("user" + id + "@iter.test")
                .password("encoded-password")
                .name("회원" + id)
                .status(status)
                .build();
    }

    private Equipment equipment(Long id, Long ownerId, EquipmentStatus status) {
        return Equipment.builder()
                .id(id)
                .ownerId(ownerId)
                .category("카메라")
                .name("테스트 장비")
                .dailyPrice(java.math.BigDecimal.valueOf(30000))
                .status(status)
                .build();
    }

    private Rental rental(Long id, Long equipmentId, Long renterId) {
        return Rental.builder()
                .id(id)
                .equipmentId(equipmentId)
                .renterId(renterId)
                .startDate(java.time.LocalDate.of(2026, 8, 1))
                .endDate(java.time.LocalDate.of(2026, 8, 5))
                .productNameSnapshot("테스트 장비")
                .categorySnapshot("카메라")
                .dailyPriceSnapshot(java.math.BigDecimal.valueOf(30000))
                .rentalDays(5)
                .totalPrice(java.math.BigDecimal.valueOf(150000))
                .build();
    }
}
