package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * 대여자가 아닌 장비 등록자(owner) 기준으로 조회
     * Rental이 Equipment와 FK 연관관계가 없어(equipmentId만 값으로 보관) 서브쿼리로 소유 장비 조회
     */
    @Query(
            "SELECT r FROM Rental r " +
            "WHERE r.equipmentId IN (SELECT e.id FROM Equipment e WHERE e.ownerId = :ownerId) " +
            "AND (:status IS NULL OR r.status = :status)"
    )
    Page<Rental> findReceivedRentals( @Param("ownerId") Long ownerId, @Param("status") RentalStatus status, Pageable pageable );

    /**
     * 승인(#6) 전용 — 같은 장비·겹치는 기간의 다른 REQUESTED 예약들.
     * approve 트랜잭션 안에서 이 목록을 자동 거절+환불 처리한다.
     */
    @Query(
            "SELECT r FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.id <> :excludeRentalId " +
            "AND r.status = RentalStatus.REQUESTED " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate"
    )
    List<Rental> findOverlappingRequestedRentals(
            @Param("equipmentId") Long equipmentId,
            @Param("excludeRentalId") Long excludeRentalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
                                                );

    /**
     * 대여 요청 생성시 선택한 기간에 이미 확정된 예약이 있는지 체크
     * - 확정 기준 : PENDING(결제 대기)/REQUESTED(승인 대기)/REJECTED/CANCELED를 제외한 나머지 상태
     */
    @Query(
            "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.status NOT IN (RentalStatus.PENDING, " +
            "                     RentalStatus.REQUESTED, " +
            "                     RentalStatus.REJECTED, " +
            "                     RentalStatus.CANCELED) " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate"
    )
    boolean existsConflictingConfirmedRental(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
                                            );

}
