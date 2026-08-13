package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    /**
     * 대여 요청 생성시 선택한 기간에 이미 확정된 예약이 있는지 체크
     * - 확정 기준 : PENDING(결제 대기)/REQUESTED(승인 대기)/REJECTED/CANCELED를 제외한 나머지 상태
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.status NOT IN (RentalStatus.PENDING, " +
            "                     RentalStatus.REQUESTED, " +
            "                     RentalStatus.REJECTED, " +
            "                     RentalStatus.CANCELED) " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate")
    boolean existsConflictingConfirmedRental(@Param("equipmentId") Long equipmentId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

}
