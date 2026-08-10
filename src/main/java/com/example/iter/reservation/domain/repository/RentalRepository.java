package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    // TODO: 기간 겹침 검증 쿼리(동일 equipmentId + 기간 겹침 + 상태 REQUESTED 이상) — 낙관적 락(@Version) 적용과 함께 담당자가 추가
}
