package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

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

    // TODO: 기간 겹침 검증 쿼리(동일 equipmentId + 기간 겹침 + 상태 REQUESTED 이상) — 낙관적 락(@Version) 적용과 함께 담당자가 추가

     // 해당 회원이 대여자인 성립된 거래 수를 조회합니다.
    long countByRenterIdAndStatusIn(Long renterId, Collection<RentalStatus> statuses);


    //해당 회원이 대여자인 현재 연체 거래 수를 조회합니다.
    long countByRenterIdAndEndDateBeforeAndStatusIn(Long renterId, LocalDate today, Collection<RentalStatus> statuses);


    // 해당 회원이 소유한 장비에서 발생한 성립된 거래 수를 조회합니다.
    @Query("""
            select count(r.id)
            from Rental r, Equipment e
            where r.equipmentId = e.id
              and e.ownerId = :ownerId
              and r.status in :statuses
            """)
    long countLentByOwnerIdAndStatusIn(
            @Param("ownerId") Long ownerId,
            @Param("statuses") Collection<RentalStatus> statuses
    );


    // 등록자가 소유한 장비의 대여 거래 중 반납 최종 확인이 필요한 거래를 조회합니다.
    @Query(
            value = """
                select r from Rental r
                join Equipment e on e.id = r.equipmentId
                where e.ownerId = :ownerId and r.status = :status
                """,
            countQuery = """
                select count(r.id) from Rental r
                join Equipment e on e.id = r.equipmentId
                where e.ownerId = :ownerId and r.status = :status
                """
    )
    Page<Rental> findReturnTargetsByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") RentalStatus status,
            Pageable pageable
    );

    // 동일 거래의 반납 최종 확인이 동시에 처리되지 않도록 거래 행을 비관적 쓰기 락으로 조회합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rental> findWithLockById(Long rentalId);
    /**
     * 대여 요청 생성시 선택한 기간에 이미 확정된 예약이 있는지 체크
     * - 확정 기준 : PENDING(결제 대기)/REQUESTED(승인 대기)/REJECTED/CANCELED를 제외한 나머지 상태
     */
    @Query(
            "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.status NOT IN :excludedStatuses " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate"
    )
    boolean existsConflictingConfirmedRental(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses
                                            );

}
