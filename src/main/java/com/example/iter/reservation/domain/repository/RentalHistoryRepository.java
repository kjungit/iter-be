package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface RentalHistoryRepository extends Repository<Rental, Long>, JpaSpecificationExecutor<Rental> {

    // findBorrowedHistory는 RentalSpecifications로 조립해서 findAll(spec, pageable)로 호출한다
    // (value/countQuery를 손으로 두 벌 유지하지 않기 위해 Specification으로 전환).

    // 등록자가 소유한 장비의 대여 거래를 상태와 예약 당시 장비명으로 검색합니다.
    // LOCATE를 사용해 %, _를 와일드카드가 아닌 실제 검색 문자로 처리합니다.
    @Query(
            value = """
                    select r
                    from Rental r
                        join Equipment e on r.equipmentId = e.id
                    where e.ownerId = :ownerId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or locate(lower(:equipmentName), lower(r.productNameSnapshot)) > 0
                          )
                    """,
            countQuery = """
                    select count(r.id)
                    from Rental r
                        join Equipment e on r.equipmentId = e.id
                    where e.ownerId = :ownerId
                      and (:status is null or r.status = :status)
                      and (
                            :equipmentName is null
                            or locate(lower(:equipmentName), lower(r.productNameSnapshot)) > 0
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
                    from Rental r
                        join Equipment e on r.equipmentId = e.id
                    where e.ownerId = :ownerId
                      and r.endDate < :today
                      and r.status in :statuses
                    """,
            countQuery = """
                    select count(r.id)
                    from Rental r
                        join Equipment e on r.equipmentId = e.id
                    where e.ownerId = :ownerId
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
