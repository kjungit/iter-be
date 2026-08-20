package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.common.config.JpaConfig;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.dispute.domain.entity.Report;
import com.example.iter.dispute.domain.entity.ReportStatus;
import com.example.iter.dispute.domain.entity.ReportTargetType;
import com.example.iter.dispute.domain.repository.ReportRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AdminUserAggregationRepositoryTest {

    private static final Set<RentalStatus> ESTABLISHED_STATUSES = EnumSet.of(
            RentalStatus.APPROVED,
            RentalStatus.SHIPPING,
            RentalStatus.RECEIVED,
            RentalStatus.RENTING,
            RentalStatus.RETURN_REQUESTED,
            RentalStatus.RETURNING,
            RentalStatus.RETURNED,
            RentalStatus.DISPUTED,
            RentalStatus.COMPLETED
    );

    private static final Set<RentalStatus> OVERDUE_STATUSES = EnumSet.of(
            RentalStatus.RECEIVED,
            RentalStatus.RENTING,
            RentalStatus.RETURN_REQUESTED,
            RentalStatus.RETURNING
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void 대여자와_등록자의_성립된_거래만_각각_집계한다() {
        User owner = userRepository.saveAndFlush(user("owner@iter.test", "등록자"));
        User renter = userRepository.saveAndFlush(user("renter@iter.test", "대여자"));
        User otherOwner = userRepository.saveAndFlush(user("other-owner@iter.test", "다른 등록자"));
        User otherRenter = userRepository.saveAndFlush(user("other-renter@iter.test", "다른 대여자"));
        Equipment ownerEquipment = equipmentRepository.saveAndFlush(equipment(owner.getId(), "등록자 장비"));
        Equipment otherEquipment = equipmentRepository.saveAndFlush(equipment(otherOwner.getId(), "다른 장비"));
        LocalDate endDate = LocalDate.now().plusDays(3);

        rentalRepository.saveAndFlush(rental(ownerEquipment.getId(), renter.getId(), endDate, RentalStatus.APPROVED));
        rentalRepository.saveAndFlush(rental(ownerEquipment.getId(), renter.getId(), endDate, RentalStatus.COMPLETED));
        rentalRepository.saveAndFlush(rental(ownerEquipment.getId(), renter.getId(), endDate, RentalStatus.REQUESTED));
        rentalRepository.saveAndFlush(rental(otherEquipment.getId(), renter.getId(), endDate, RentalStatus.RENTING));
        rentalRepository.saveAndFlush(rental(ownerEquipment.getId(), otherRenter.getId(), endDate, RentalStatus.RETURNED));

        long rentedCount = rentalRepository.countByRenterIdAndStatusIn(
                renter.getId(),
                ESTABLISHED_STATUSES
        );
        long lentCount = rentalRepository.countLentByOwnerIdAndStatusIn(
                owner.getId(),
                ESTABLISHED_STATUSES
        );

        assertThat(rentedCount).isEqualTo(3L);
        assertThat(lentCount).isEqualTo(3L);
    }

    @Test
    void 종료일이_지났고_아직_반환되지_않은_대여만_연체로_집계한다() {
        User owner = userRepository.saveAndFlush(user("overdue-owner@iter.test", "등록자"));
        User renter = userRepository.saveAndFlush(user("overdue-renter@iter.test", "대여자"));
        Equipment equipment = equipmentRepository.saveAndFlush(equipment(owner.getId(), "연체 장비"));
        LocalDate today = LocalDate.now();

        rentalRepository.saveAndFlush(rental(equipment.getId(), renter.getId(), today.minusDays(5), RentalStatus.RECEIVED));
        rentalRepository.saveAndFlush(rental(equipment.getId(), renter.getId(), today.minusDays(1), RentalStatus.RETURNING));
        rentalRepository.saveAndFlush(rental(equipment.getId(), renter.getId(), today, RentalStatus.RENTING));
        rentalRepository.saveAndFlush(rental(equipment.getId(), renter.getId(), today.minusDays(2), RentalStatus.COMPLETED));
        rentalRepository.saveAndFlush(rental(equipment.getId(), renter.getId(), today.plusDays(1), RentalStatus.RECEIVED));

        long overdueCount = rentalRepository.countByRenterIdAndEndDateBeforeAndStatusIn(
                renter.getId(),
                today,
                OVERDUE_STATUSES
        );

        assertThat(overdueCount).isEqualTo(2L);
    }

    @Test
    void 회원을_대상으로_접수된_신고만_신고자와_관계없이_집계한다() {
        User target = userRepository.saveAndFlush(user("target@iter.test", "신고 대상"));
        User firstReporter = userRepository.saveAndFlush(user("reporter-one@iter.test", "신고자 1"));
        User secondReporter = userRepository.saveAndFlush(user("reporter-two@iter.test", "신고자 2"));

        reportRepository.saveAndFlush(report(firstReporter.getId(), ReportTargetType.USER, target.getId()));
        reportRepository.saveAndFlush(report(secondReporter.getId(), ReportTargetType.USER, target.getId()));
        reportRepository.saveAndFlush(report(firstReporter.getId(), ReportTargetType.EQUIPMENT, target.getId()));
        reportRepository.saveAndFlush(report(firstReporter.getId(), ReportTargetType.USER, secondReporter.getId()));

        long reportCount = reportRepository.countByTargetTypeAndTargetId(
                ReportTargetType.USER,
                target.getId()
        );

        assertThat(reportCount).isEqualTo(2L);
    }

    private User user(String email, String name) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .nickname(name)
                .phone("010-0000-0000")
                .build();
    }

    private Equipment equipment(Long ownerId, String name) {
        return Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.LAPTOP)
                .name(name)
                .description("테스트 장비")
                .dailyPrice(BigDecimal.valueOf(10_000))
                .availableFrom(LocalDate.now().minusMonths(1))
                .availableTo(LocalDate.now().plusMonths(1))
                .build();
    }

    private Rental rental(
            Long equipmentId,
            Long renterId,
            LocalDate endDate,
            RentalStatus status
    ) {
        return Rental.builder()
                .equipmentId(equipmentId)
                .renterId(renterId)
                .startDate(endDate.minusDays(3))
                .endDate(endDate)
                .productNameSnapshot("테스트 장비")
                .categorySnapshot("노트북")
                .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                .rentalDays(4)
                .totalPrice(BigDecimal.valueOf(40_000))
                .status(status)
                .build();
    }

    private Report report(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    ) {
        return Report.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason("테스트 신고")
                .description("테스트 신고 내용입니다.")
                .status(ReportStatus.RECEIVED)
                .build();
    }
}
