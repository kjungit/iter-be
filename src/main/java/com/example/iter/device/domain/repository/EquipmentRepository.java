package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
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
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByIdAndStatus(Long id, EquipmentStatus status);

    @Query(
            value = """
                    select e
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
    Page<Equipment> searchPublicEquipment(
            @Param("keyword") String keyword,
            @Param("category") EquipmentCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatuses") Collection<RentalStatus> excludedStatuses,
            Pageable pageable
    );

    Page<Equipment> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Equipment> findByOwnerIdAndStatus(
            Long ownerId,
            EquipmentStatus status,
            Pageable pageable
    );

    /**
     * 대여 생성·승인 시 SELECT ... FOR UPDATE로 같은 장비에 대한 요청을 직렬화합니다.
     * 트랜잭션 안에서 장비 상태와 기간 충돌을 재검증한 뒤 상태 변경을 수행합니다.
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
          and (
                :cursorCreatedAt is null
                or e.createdAt < :cursorCreatedAt
                or (e.createdAt = :cursorCreatedAt and e.id < :cursorId)
              )
        order by e.createdAt desc, e.id desc
        """)
    List<Equipment> searchForAdminByCursor(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") EquipmentStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
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
