package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * 대여자가 아닌 장비 등록자(owner) 기준으로 조회
     * Rental이 Equipment와 FK 연관관계가 없어(equipmentId만 값으로 보관) 서브쿼리로 소유 장비 조회
     */
    @Query("""
            select r
            from Rental r
            where r.equipmentId in (select e.id from Equipment e where e.ownerId = :ownerId)
              and (:status is null or r.status = :status)
            """)
    Page<Rental> findReceivedRentals( @Param("ownerId") Long ownerId, @Param("status") RentalStatus status, Pageable pageable );

    // 결제(#3)를 30분 안에 완료하지 않은 PENDING 요청을 자동 취소해서 선점을 풀어준다.
    @Modifying
    @Query("""
            update Rental r
            set r.status = RentalStatus.CANCELED
            where r.status = RentalStatus.PENDING
              and r.createdAt < :cutoff
            """)
    int expirePendingRentals(@Param("cutoff") LocalDateTime cutoff);

     // 해당 회원이 대여자인 성립된 거래 수를 조회합니다.
    long countByRenterIdAndStatusIn(Long renterId, Collection<RentalStatus> statuses);


    //해당 회원이 대여자인 현재 연체 거래 수를 조회합니다.
    long countByRenterIdAndEndDateBeforeAndStatusIn(Long renterId, LocalDate today, Collection<RentalStatus> statuses);


    // 해당 회원이 소유한 장비에서 발생한 성립된 거래 수를 조회합니다.
    @Query("""
            select count(r.id)
            from Rental r
            join Equipment e on e.id = r.equipmentId
            where e.ownerId = :ownerId
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
    /** 제외 상태를 제외하고 선택한 기간과 겹치는 예약 ID를 최대 한 건 조회합니다. */
    @Query(
            "SELECT r.id " +
            "FROM Rental r " +
            "WHERE r.equipmentId = :equipmentId " +
            "AND r.status NOT IN :excludedStatuses " +
            "AND r.startDate <= :endDate " +
            "AND r.endDate >= :startDate"
    )
    List<Long> findConflictingOccupyingRentalIds(
    /** 제외 상태를 제외하고 선택한 기간과 겹치는 예약이 있는지 확인합니다. */
    @Query("""
            select case when count(r) > 0 then true else false end
            from Rental r
            where r.equipmentId = :equipmentId
              and r.status not in :excludedStatuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    boolean existsConflictingOccupyingRental(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses,
            Pageable pageable
    );

    /** 충돌 행 전체를 세지 않고 첫 번째 ID가 발견되면 조회를 종료합니다. */
    default boolean existsConflictingOccupyingRental(
            Long equipmentId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<RentalStatus> excludedStatuses
    ) {
        return !findConflictingOccupyingRentalIds(
                equipmentId,
                startDate,
                endDate,
                excludedStatuses,
                PageRequest.of(0, 1)
        ).isEmpty();
    }

    /**
     * 겹침 확인용 락 읽기 버전. equipment를 FOR UPDATE로 잠근 뒤에도 이 조회가 일반 SELECT면
     * MySQL REPEATABLE READ 트랜잭션의 최초 스냅샷을 그대로 보므로, 락을 기다리는 동안 다른
     * 트랜잭션이 커밋한 겹치는 예약을 놓칠 수 있다. PESSIMISTIC_READ로 최신 커밋 데이터를
     * 강제로 다시 읽어야 하므로, 이미 equipment 락을 잡은 createRental/approveRental에서만 쓴다
     * (평상시 가용성 조회는 락이 필요 없어 existsConflictingOccupyingRental을 그대로 쓴다).
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select r
            from Rental r
            where r.equipmentId = :equipmentId
              and r.status not in :excludedStatuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<Rental> findConflictingOccupyingRentalsForUpdate(
            @Param("equipmentId") Long equipmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses
                                            );

    boolean existsByEquipmentIdAndStatusIn(
            Long equipmentId,
            Collection<RentalStatus> statuses
    );

    boolean existsByEquipmentIdAndStatus(Long equipmentId, RentalStatus status);

    @Query("""
            select r.id
            from Rental r
            where r.equipmentId = :equipmentId
              and r.status not in :excludedStatuses
              and (r.startDate < :availableFrom or r.endDate > :availableTo)
            """)
    List<Long> findOccupyingRentalOutsidePeriodIds(
            @Param("equipmentId") Long equipmentId,
            @Param("availableFrom") LocalDate availableFrom,
            @Param("availableTo") LocalDate availableTo,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses,
            Pageable pageable
    );

    /** 허용 기간을 벗어난 점유 거래도 첫 번째 ID만 확인합니다. */
    default boolean existsOccupyingRentalOutsidePeriod(
            Long equipmentId,
            LocalDate availableFrom,
            LocalDate availableTo,
            Collection<RentalStatus> excludedStatuses
    ) {
        return !findOccupyingRentalOutsidePeriodIds(
                equipmentId,
                availableFrom,
                availableTo,
                excludedStatuses,
                PageRequest.of(0, 1)
        ).isEmpty();
    }

    @Query("""
            select r
            from Rental r
            where r.equipmentId = :equipmentId
              and r.status not in :excludedStatuses
              and r.startDate <= :to
              and r.endDate >= :from
            order by r.startDate asc, r.endDate asc, r.id asc
            """)
    List<Rental> findEquipmentSchedule(
            @Param("equipmentId") Long equipmentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses
    );

}
