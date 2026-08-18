package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    // TODO: 카테고리/가격/기간 검색, 정렬, 활성 상태 필터링 등은 QueryDSL 도입 여부와 함께 팀 논의 필요

    /**
     * 대여 승인(#6) 전용 — SELECT ... FOR UPDATE로 equipment 행을 잠근다.
     * 같은 장비에 대한 동시 승인 요청이 이 락을 순차적으로 기다리게 해서,
     * 트랜잭션 안에서 "재검증 -> 승인/충돌 판단 -> 경쟁 REQUESTED 자동거절"을 원자적으로 만든다.
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") Long id);

    // 관리자가 장비명, 카테고리, 상태 조건으로 전체 장비를 조회합니다.
    // 전달되지 않은 조건은 조회에 적용하지 않습니다.
    @Query("""
        select e
        from Equipment e
        where (
                :keyword is null
                or lower(e.name)
                    like lower(concat('%', :keyword, '%'))
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

}
