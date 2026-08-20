package com.example.iter.device.util;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEquipmentMapperTest {

    private final AdminEquipmentMapper adminEquipmentMapper = new AdminEquipmentMapper();

    @Test
    void 장비와_등록자와_썸네일을_목록_응답으로_변환한다() {
        Equipment equipment = equipment();
        User owner = owner();

        var response = adminEquipmentMapper.toSummary(
                equipment,
                owner,
                "https://example.com/thumbnail.jpg"
        );

        assertThat(response.equipmentId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("맥북 프로");
        assertThat(response.category()).isEqualTo(EquipmentCategory.LAPTOP);
        assertThat(response.dailyPrice()).isEqualByComparingTo("30000");
        assertThat(response.status()).isEqualTo(EquipmentStatus.SUSPENDED);
        assertThat(response.owner().userId()).isEqualTo(2L);
        assertThat(response.owner().nickName()).isEqualTo("등록자");
        assertThat(response.thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 0));
    }

    @Test
    void 장비와_등록자와_이미지를_상세_응답으로_변환한다() {
        Equipment equipment = equipment();
        User owner = owner();
        EquipmentImage image = EquipmentImage.builder()
                .id(100L)
                .equipment(equipment)
                .imageUrl("https://example.com/image.jpg")
                .sortOrder(1)
                .thumbnail(true)
                .build();

        var response = adminEquipmentMapper.toDetail(
                equipment,
                owner,
                List.of(image)
        );

        assertThat(response.equipmentId()).isEqualTo(10L);
        assertThat(response.owner().userId()).isEqualTo(2L);
        assertThat(response.description()).isEqualTo("테스트 장비");
        assertThat(response.availableFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.availableTo()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(response.productCondition()).isEqualTo(ProductConditionType.NORMAL);
        assertThat(response.conditionDetail()).isEqualTo("정상");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().imageId()).isEqualTo(100L);
        assertThat(response.images().getFirst().thumbnail()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 2, 11, 0));
    }

    private Equipment equipment() {
        Equipment equipment = Equipment.builder()
                .id(10L)
                .ownerId(2L)
                .category(EquipmentCategory.LAPTOP)
                .name("맥북 프로")
                .description("테스트 장비")
                .dailyPrice(BigDecimal.valueOf(30000))
                .availableFrom(LocalDate.of(2026, 8, 1))
                .availableTo(LocalDate.of(2026, 8, 31))
                .status(EquipmentStatus.SUSPENDED)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("정상")
                .build();
        ReflectionTestUtils.setField(
                equipment,
                "createdAt",
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        ReflectionTestUtils.setField(
                equipment,
                "updatedAt",
                LocalDateTime.of(2026, 8, 2, 11, 0)
        );
        return equipment;
    }

    private User owner() {
        return User.builder()
                .id(2L)
                .email("owner@iter.test")
                .password("encoded-password")
                .name("장비 등록자")
                .nickname("등록자")
                .phone("010-0000-0002")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
