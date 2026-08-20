package com.example.iter.device.domain.repository;

import com.example.iter.common.config.JpaConfig;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class EquipmentRepositoryAdminTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 장비명을_대소문자_구분_없이_검색한다() {
        Equipment first = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.LAPTOP,
                "MacBook Pro",
                EquipmentStatus.ACTIVE
        ));
        Equipment second = equipmentRepository.saveAndFlush(equipment(
                2L,
                EquipmentCategory.LAPTOP,
                "MACBOOK Air",
                EquipmentStatus.SUSPENDED
        ));
        equipmentRepository.saveAndFlush(equipment(
                3L,
                EquipmentCategory.CAMERA,
                "소니 카메라",
                EquipmentStatus.ACTIVE
        ));

        var result = equipmentRepository.searchForAdmin(
                "macbook",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(result.getContent())
                .extracting(Equipment::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void 퍼센트_기호를_와일드카드가_아닌_일반_문자로_검색한다() {
        Equipment expected = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.OTHER,
                "배터리 100% 충전기",
                EquipmentStatus.ACTIVE
        ));
        equipmentRepository.saveAndFlush(equipment(
                2L,
                EquipmentCategory.OTHER,
                "배터리 완전 충전기",
                EquipmentStatus.ACTIVE
        ));

        var result = equipmentRepository.searchForAdmin(
                "%",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(result.getContent())
                .extracting(Equipment::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void 언더스코어를_와일드카드가_아닌_일반_문자로_검색한다() {
        Equipment expected = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.CAMERA,
                "EOS_R5 카메라",
                EquipmentStatus.ACTIVE
        ));
        equipmentRepository.saveAndFlush(equipment(
                2L,
                EquipmentCategory.CAMERA,
                "EOSXR5 카메라",
                EquipmentStatus.ACTIVE
        ));

        var result = equipmentRepository.searchForAdmin(
                "_",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(result.getContent())
                .extracting(Equipment::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void 카테고리와_상태를_모두_적용해_조회한다() {
        Equipment expected = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.CAMERA,
                "소니 카메라",
                EquipmentStatus.SUSPENDED
        ));
        equipmentRepository.saveAndFlush(equipment(
                2L,
                EquipmentCategory.CAMERA,
                "캐논 카메라",
                EquipmentStatus.ACTIVE
        ));
        equipmentRepository.saveAndFlush(equipment(
                3L,
                EquipmentCategory.LAPTOP,
                "카메라 편집 노트북",
                EquipmentStatus.SUSPENDED
        ));

        var result = equipmentRepository.searchForAdmin(
                null,
                "camera",
                EquipmentStatus.SUSPENDED,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(Equipment::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void 검색_조건이_없으면_삭제된_장비를_포함해_전체_장비를_페이징한다() {
        Equipment first = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.LAPTOP,
                "첫 번째 장비",
                EquipmentStatus.ACTIVE
        ));
        Equipment second = equipmentRepository.saveAndFlush(equipment(
                2L,
                EquipmentCategory.CAMERA,
                "두 번째 장비",
                EquipmentStatus.DELETED
        ));

        var firstPage = equipmentRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "id"))
        );
        var secondPage = equipmentRepository.searchForAdmin(
                null,
                null,
                null,
                PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(Equipment::getId)
                .containsExactly(first.getId());
        assertThat(secondPage.getContent())
                .extracting(Equipment::getId)
                .containsExactly(second.getId());
    }

    @Test
    void 장비_상태_변경용_조회는_비관적_쓰기_락을_획득한다() {
        Equipment saved = equipmentRepository.saveAndFlush(equipment(
                1L,
                EquipmentCategory.LAPTOP,
                "락 대상 장비",
                EquipmentStatus.ACTIVE
        ));
        entityManager.clear();

        Equipment found = equipmentRepository.findByIdForUpdate(saved.getId())
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(entityManager.getLockMode(found))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private Equipment equipment(
            Long ownerId,
            EquipmentCategory category,
            String name,
            EquipmentStatus status
    ) {
        return Equipment.builder()
                .ownerId(ownerId)
                .category(category)
                .name(name)
                .description("테스트 장비")
                .dailyPrice(BigDecimal.valueOf(30000))
                .availableFrom(LocalDate.of(2026, 8, 1))
                .availableTo(LocalDate.of(2026, 8, 31))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("정상")
                .build();
    }
}
