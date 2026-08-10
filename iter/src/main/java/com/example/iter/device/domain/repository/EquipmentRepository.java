package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    // TODO: 카테고리/가격/기간 검색, 정렬, 활성 상태 필터링 등은 QueryDSL 도입 여부와 함께 팀 논의 필요
}
