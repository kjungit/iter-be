package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface RentalHistoryRepository extends Repository<Rental, Long> {


    // 대여자가 빌린 거래를 상태와 예약 당시 장비명으로 검색합니다.
    @Query(
            value = """
                    select r
                    from Rental r
                    where r.renterId = :renterId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or lower(r.productNameSnapshot)
                                like lower(concat('%', :equipmentName, '%'))
                          )
                    """,
            countQuery = """
                    select count(r.id)
                    from Rental r
                    where r.renterId = :renterId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or lower(r.productNameSnapshot)
                                like lower(concat('%', :equipmentName, '%'))
                          )
                    """
    )
    Page<Rental> findBorrowedHistory(
            @Param("renterId") Long renterId,
            @Param("status") RentalStatus status,
            @Param("equipmentName") String equipmentName,
            Pageable pageable
    );


    // 등록자가 소유한 장비의 대여 거래를 상태와 예약 당시 장비명으로 검색합니다.
    @Query(
            value = """
                    select r
                    from Rental r, Equipment e
                    where r.equipmentId = e.id
                      and e.ownerId = :ownerId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or lower(r.productNameSnapshot)
                                like lower(concat('%', :equipmentName, '%'))
                          )
                    """,
            countQuery = """
                    select count(r.id)
                    from Rental r, Equipment e
                    where r.equipmentId = e.id
                      and e.ownerId = :ownerId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or lower(r.productNameSnapshot)
                                like lower(concat('%', :equipmentName, '%'))
                          )
                    """
    )
    Page<Rental> findLentHistory(
            @Param("ownerId") Long ownerId,
            @Param("status") RentalStatus status,
            @Param("equipmentName") String equipmentName,
            Pageable pageable
    );

    // 대여자가 아직 반납을 마치지 못한 연체 거래를 조회합니다.
    Page<Rental> findByRenterIdAndEndDateBeforeAndStatusIn(Long renterId, LocalDate today, Collection<RentalStatus> statuses, Pageable pageable);

    // 등록자가 빌려준 장비 중 아직 반납되지 않은 연체 거래를 조회합니다.
    @Query(
            value = """
                    select r
                    from Rental r, Equipment e
                    where r.equipmentId = e.id
                      and e.ownerId = :ownerId
                      and r.endDate < :today
                      and r.status in :statuses
                    """,
            countQuery = """
                    select count(r.id)
                    from Rental r, Equipment e
                    where r.equipmentId = e.id
                      and e.ownerId = :ownerId
                      and r.endDate < :today
                      and r.status in :statuses
                    """
    )
    Page<Rental> findLentOverdueHistory(
            @Param("ownerId") Long ownerId,
            @Param("today") LocalDate today,
            @Param("statuses") Collection<RentalStatus> statuses,
            Pageable pageable
    );
}
