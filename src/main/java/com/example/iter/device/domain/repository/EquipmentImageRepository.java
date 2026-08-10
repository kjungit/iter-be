package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.EquipmentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentImageRepository extends JpaRepository<EquipmentImage, Long> {
    List<EquipmentImage> findByEquipmentIdOrderBySortOrderAsc(Long equipmentId);
}
