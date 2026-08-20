package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.service.model.EquipmentDetailRow;
import com.example.iter.device.service.model.EquipmentSearchRow;
import com.example.iter.reservation.domain.entity.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("""
            select new com.example.iter.device.service.model.EquipmentDetailRow(
                e,
                coalesce(avg(r.rating), 0.0),
                count(r.id)
            )
            from Equipment e
            left join Review r on r.equipment = e
            where e.id = :equipmentId
              and e.status = EquipmentStatus.ACTIVE
            group by e
            """)
    Optional<EquipmentDetailRow> findPublicDetailById(@Param("equipmentId") Long equipmentId);

    @Query(
            value = """
                    select new com.example.iter.device.service.model.EquipmentSearchRow(
                        e,
                        coalesce(avg(r.rating), 0.0),
                        count(r.id)
                    )
                    from Equipment e
                    left join Review r on r.equipment = e
                    where e.status = EquipmentStatus.ACTIVE
                      and (:keyword is null or lower(e.name) like lower(concat('%', :keyword, '%')) escape '\\')
                      and (:category is null or e.category = :category)
                      and (:minPrice is null or e.dailyPrice >= :minPrice)
                      and (:maxPrice is null or e.dailyPrice <= :maxPrice)
                      and (
                          :startDate is null
                          or (
                              e.availableFrom <= :startDate
                              and e.availableTo >= :endDate
                              and not exists (
                                  select rental.id
                                  from Rental rental
                                  where rental.equipmentId = e.id
                                    and rental.status not in :excludedStatuses
                                    and rental.startDate <= :endDate
                                    and rental.endDate >= :startDate
                              )
                          )
                      )
                    group by e
                    order by
                      case when :sort = 'LATEST' then e.createdAt end desc,
                      case when :sort = 'PRICE_ASC' then e.dailyPrice end asc,
                      case when :sort = 'PRICE_DESC' then e.dailyPrice end desc,
                      case when :sort = 'RATING_DESC' then coalesce(avg(r.rating), 0.0) end desc,
                      case when :sort = 'RATING_DESC' then count(r.id) end desc,
                      e.id desc
                    """,
            countQuery = """
                    select count(e.id)
                    from Equipment e
                    where e.status = EquipmentStatus.ACTIVE
                      and (:keyword is null or lower(e.name) like lower(concat('%', :keyword, '%')) escape '\\')
                      and (:category is null or e.category = :category)
                      and (:minPrice is null or e.dailyPrice >= :minPrice)
                      and (:maxPrice is null or e.dailyPrice <= :maxPrice)
                      and (
                          :startDate is null
                          or (
                              e.availableFrom <= :startDate
                              and e.availableTo >= :endDate
                              and not exists (
                                  select rental.id
                                  from Rental rental
                                  where rental.equipmentId = e.id
                                    and rental.status not in :excludedStatuses
                                    and rental.startDate <= :endDate
                                    and rental.endDate >= :startDate
                              )
                          )
                      )
                    """
    )
    Page<EquipmentSearchRow> searchPublicEquipment(
            @Param("keyword") String keyword,
            @Param("category") EquipmentCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses,
            @Param("sort") String sort,
            Pageable pageable
    );

    /**
     * 대여 승인(#6) 전용 — SELECT ... FOR UPDATE로 equipment 행을 잠근다.
     * 같은 장비에 대한 동시 승인 요청이 이 락을 순차적으로 기다리게 해서,
     * 트랜잭션 안에서 "재검증 -> 승인/충돌 판단 -> 경쟁 REQUESTED 자동거절"을 원자적으로 만든다.
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") Long id);

    // 관리자가 장비명, 카테고리, 상태 조건으로 전체 장비를 조회합니다.
    // LOCATE를 사용해 %, _ 등의 문자를 와일드카드가 아닌 실제 검색 문자로 처리합니다.
    // 전달되지 않은 조건은 조회에 적용하지 않습니다.
    @Query("""
        select e
        from Equipment e
        where (
                :keyword is null
                or locate(lower(:keyword), lower(e.name)) > 0
              )
          and (
                :category is null
                or lower(e.category) = lower(:category)
              )
          and (
                :status is null
                or e.status = :status
              )
        """)
    Page<Equipment> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") EquipmentStatus status,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update Equipment e
            set e.status = :status, e.updatedAt = :updatedAt
            where e.ownerId = :ownerId and e.status <> :status
            """)
    int updateStatusByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("status") EquipmentStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
