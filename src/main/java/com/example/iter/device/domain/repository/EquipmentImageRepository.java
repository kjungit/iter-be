package com.example.iter.device.domain.repository;

import com.example.iter.device.domain.entity.EquipmentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EquipmentImageRepository extends JpaRepository<EquipmentImage, Long> {
    List<EquipmentImage> findByEquipmentIdOrderBySortOrderAsc(Long equipmentId);

    List<EquipmentImage> findByEquipmentIdOrderBySortOrderAscIdAsc(Long equipmentId);

    // 여러 장비의 썸네일을 한 번에 조회합니다.
    List<EquipmentImage>
    findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(Collection<Long> equipmentIds);
}
