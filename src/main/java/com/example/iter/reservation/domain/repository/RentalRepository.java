package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 대여 요청 생성(#1) 전용 — 선점 방식: REJECTED/CANCELED를 제외한 모든 상태(PENDING/REQUESTED 포함)와
     * 겹치는지 확인한다. 새로 만드는 요청이라 자기 자신을 제외할 필요가 없다.
     * createRental()에서 Equipment 락을 잡은 뒤 호출해야 동시 요청 레이스가 막힌다.
     */
    @Query(
            "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.status NOT IN (RentalStatus.REJECTED, RentalStatus.CANCELED) " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate"
    )
    boolean existsConflictingActiveRental(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
                                          );

    // 결제(#3)를 30분 안에 완료하지 않은 PENDING 요청을 자동 취소해서 선점을 풀어준다.
    @Modifying
    @Query("UPDATE Rental r SET r.status = RentalStatus.CANCELED " +
            "WHERE r.status = RentalStatus.PENDING AND r.createdAt < :cutoff")
    int expirePendingRentals(@Param("cutoff") LocalDateTime cutoff);

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
