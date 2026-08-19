package com.example.iter.reservation.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.util.RentalOverduePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RentalHistoryRepositoryTest {

    private static final Long RENTER_ID = 101L;
    private static final Long OTHER_RENTER_ID = 102L;
    private static final Long OWNER_ID = 201L;
    private static final Long OTHER_OWNER_ID = 202L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Autowired
    private RentalHistoryRepository rentalHistoryRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void 빌린_장비_이력은_해당_대여자의_거래만_조회한다() {
        Equipment equipment = equipment(OWNER_ID, "장비");
        Rental first = rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "첫 장비", TODAY);
        Rental second = rental(equipment.getId(), RENTER_ID, RentalStatus.RENTING, "둘째 장비", TODAY.plusDays(1));
        rental(equipment.getId(), OTHER_RENTER_ID, RentalStatus.COMPLETED, "다른 회원 장비", TODAY);

        var result = rentalHistoryRepository.findBorrowedHistory(
                RENTER_ID,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.getContent())
                .allMatch(rental -> rental.getRenterId().equals(RENTER_ID));
    }

    @Test
    void 빌린_이력은_상태와_예약_당시_장비명으로_검색한다() {
        Equipment equipment = equipment(OWNER_ID, "현재 변경된 카메라 이름");
        Rental matching = rental(
                equipment.getId(),
                RENTER_ID,
                RentalStatus.COMPLETED,
                "예약 당시 MacBook Pro 14",
                TODAY
        );
        rental(
                equipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "예약 당시 MacBook Air",
                TODAY
        );
        rental(
                equipment.getId(),
                RENTER_ID,
                RentalStatus.COMPLETED,
                "예약 당시 Sony Camera",
                TODAY
        );

        var result = rentalHistoryRepository.findBorrowedHistory(
                RENTER_ID,
                RentalStatus.COMPLETED,
                "macbook pro",
                PageRequest.of(0, 20)
        );
        var currentNameResult = rentalHistoryRepository.findBorrowedHistory(
                RENTER_ID,
                null,
                "현재 변경된 카메라",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(matching.getId());
        assertThat(currentNameResult).isEmpty();
    }

    @Test
    void 빌려준_장비_이력은_소유자와_검색_조건이_모두_일치하는_거래만_조회한다() {
        Equipment ownerEquipment = equipment(OWNER_ID, "소유자 장비");
        Equipment otherOwnerEquipment = equipment(OTHER_OWNER_ID, "다른 소유자 장비");
        Rental matching = rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "Sony A7C 카메라",
                TODAY.plusDays(1)
        );
        rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.COMPLETED,
                "Sony A7C 카메라",
                TODAY
        );
        rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "MacBook Pro",
                TODAY.plusDays(1)
        );
        rental(
                otherOwnerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "Sony A7C 카메라",
                TODAY.plusDays(1)
        );

        var result = rentalHistoryRepository.findLentHistory(
                OWNER_ID,
                RentalStatus.RENTING,
                "SONY",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(matching.getId());
    }

    @Test
    void 빌린_장비_연체_이력은_종료일이_지났고_반납이_끝나지_않은_거래만_조회한다() {
        Equipment equipment = equipment(OWNER_ID, "연체 장비");
        Rental received = rental(equipment.getId(), RENTER_ID, RentalStatus.RECEIVED, "수령", TODAY.minusDays(4));
        Rental renting = rental(equipment.getId(), RENTER_ID, RentalStatus.RENTING, "대여 중", TODAY.minusDays(3));
        Rental returnRequested = rental(equipment.getId(), RENTER_ID, RentalStatus.RETURN_REQUESTED, "반납 신청", TODAY.minusDays(2));
        Rental returning = rental(equipment.getId(), RENTER_ID, RentalStatus.RETURNING, "반송 중", TODAY.minusDays(1));
        rental(equipment.getId(), RENTER_ID, RentalStatus.RENTING, "오늘 종료", TODAY);
        rental(equipment.getId(), RENTER_ID, RentalStatus.RETURNED, "도착 확인", TODAY.minusDays(1));
        rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "완료", TODAY.minusDays(10));
        rental(equipment.getId(), OTHER_RENTER_ID, RentalStatus.RENTING, "다른 회원", TODAY.minusDays(5));

        var result = rentalHistoryRepository.findByRenterIdAndEndDateBeforeAndStatusIn(
                RENTER_ID,
                TODAY,
                RentalOverduePolicy.statuses(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "endDate"))
        );

        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(
                        received.getId(),
                        renting.getId(),
                        returnRequested.getId(),
                        returning.getId()
                );
    }

    @Test
    void 빌려준_장비_연체_이력은_해당_등록자의_장비만_조회한다() {
        Equipment ownerEquipment = equipment(OWNER_ID, "소유 장비");
        Equipment otherOwnerEquipment = equipment(OTHER_OWNER_ID, "다른 장비");
        Rental ownerOverdue = rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RETURNING,
                "소유 장비 연체",
                TODAY.minusDays(2)
        );
        rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RETURNED,
                "이미 반납",
                TODAY.minusDays(3)
        );
        rental(
                ownerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "오늘 종료",
                TODAY
        );
        rental(
                otherOwnerEquipment.getId(),
                RENTER_ID,
                RentalStatus.RENTING,
                "다른 소유자 연체",
                TODAY.minusDays(5)
        );

        var result = rentalHistoryRepository.findLentOverdueHistory(
                OWNER_ID,
                TODAY,
                RentalOverduePolicy.statuses(),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(ownerOverdue.getId());
    }

    @Test
    void 페이지_크기와_전체_건수를_정확히_반환한다() {
        Equipment equipment = equipment(OWNER_ID, "페이지 장비");
        rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "장비1", TODAY);
        rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "장비2", TODAY);
        rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "장비3", TODAY);
        Rental fourth = rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "장비4", TODAY);
        Rental fifth = rental(equipment.getId(), RENTER_ID, RentalStatus.COMPLETED, "장비5", TODAY);

        var result = rentalHistoryRepository.findBorrowedHistory(
                RENTER_ID,
                null,
                null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Rental::getId)
                .containsExactly(fifth.getId(), fourth.getId());
    }

    private Equipment equipment(Long ownerId, String name) {
        return equipmentRepository.saveAndFlush(
                Equipment.builder()
                        .ownerId(ownerId)
                        .category("디지털기기")
                        .name(name)
                        .description("테스트 장비")
                        .dailyPrice(BigDecimal.valueOf(20_000))
                        .availableFrom(TODAY.minusMonths(1))
                        .availableTo(TODAY.plusMonths(1))
                        .status(EquipmentStatus.ACTIVE)
                        .productCondition(ProductConditionType.NORMAL)
                        .build()
        );
    }

    private Rental rental(
            Long equipmentId,
            Long renterId,
            RentalStatus status,
            String productNameSnapshot,
            LocalDate endDate
    ) {
        return rentalRepository.saveAndFlush(
                Rental.builder()
                        .equipmentId(equipmentId)
                        .renterId(renterId)
                        .startDate(endDate.minusDays(2))
                        .endDate(endDate)
                        .productNameSnapshot(productNameSnapshot)
                        .categorySnapshot("디지털기기")
                        .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                        .rentalDays(3)
                        .totalPrice(BigDecimal.valueOf(30_000))
                        .status(status)
                        .build()
        );
    }
}
