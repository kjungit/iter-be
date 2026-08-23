package com.example.iter.device.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentDetailApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private EquipmentImageRepository equipmentImageRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @BeforeEach
    void setUp() {
        equipmentImageRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    void 공개_장비의_상세_이미지와_소유자를_인증_없이_조회한다() throws Exception {
        User owner = saveOwner("카메라주인");
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);
        saveImage(equipment, "https://example.com/images/second.jpg", 1, false);
        EquipmentImage first = saveImage(
                equipment, "https://example.com/images/first.jpg", 0, true);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equipment.getId()))
                .andExpect(jsonPath("$.name").value("소니 A7C2"))
                .andExpect(jsonPath("$.category").value("CAMERA"))
                .andExpect(jsonPath("$.description").value("풀프레임 미러리스 카메라입니다."))
                .andExpect(jsonPath("$.dailyPrice").value(30000))
                .andExpect(jsonPath("$.availableFrom").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.availableTo").value(LocalDate.now().plusMonths(2).toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.productCondition").value("NORMAL"))
                .andExpect(jsonPath("$.conditionDetail").value("사용감이 적습니다."))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].id").value(first.getId()))
                .andExpect(jsonPath("$.images[0].imageUrl")
                        .value("https://example.com/images/first.jpg"))
                .andExpect(jsonPath("$.images[0].sortOrder").value(0))
                .andExpect(jsonPath("$.images[0].thumbnail").value(true))
                .andExpect(jsonPath("$.images[1].sortOrder").value(1))
                .andExpect(jsonPath("$.owner.id").value(owner.getId()))
                .andExpect(jsonPath("$.owner.nickname").value("카메라주인"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void 이미지가_없으면_빈_목록과_비활성화된_리뷰_기본값을_반환한다() throws Exception {
        User owner = saveOwner("장비주인");
        Equipment equipment = saveEquipment(owner.getId(), EquipmentStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void 공개_상태가_아닌_장비는_존재_여부를_숨기고_404를_반환한다(EquipmentStatus status) throws Exception {
        User owner = saveOwner("비공개주인-" + status.name());
        Equipment equipment = saveEquipment(owner.getId(), status);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않거나 조회할 수 없는 장비입니다."));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void 소유자_본인은_비공개_상태여도_상세_조회할_수_있다(EquipmentStatus status) throws Exception {
        User owner = saveOwner("비공개주인-본인-" + status.name());
        Equipment equipment = saveEquipment(owner.getId(), status);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equipment.getId()))
                .andExpect(jsonPath("$.status").value(status.name()));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void 소유자가_아니면_로그인해도_비공개_장비는_404를_반환한다(EquipmentStatus status) throws Exception {
        User owner = saveOwner("비공개주인-타인-" + status.name());
        Equipment equipment = saveEquipment(owner.getId(), status);
        User outsider = saveOwner("제3자");

        mockMvc.perform(get("/api/v1/devices/{equipmentId}", equipment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void 존재하지_않는_장비는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/devices/{equipmentId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않거나 조회할 수 없는 장비입니다."));
    }

    private User saveOwner(String nickname) {
        return userRepository.saveAndFlush(User.builder()
                .email("detail-owner-" + System.nanoTime() + "@example.com")
                .password("encoded-password")
                .name("장비 소유자")
                .nickname(nickname)
                .phone("010-1111-2222")
                .build());
    }

    private Equipment saveEquipment(Long ownerId, EquipmentStatus status) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.CAMERA)
                .name("소니 A7C2")
                .description("풀프레임 미러리스 카메라입니다.")
                .dailyPrice(BigDecimal.valueOf(30_000))
                .availableFrom(LocalDate.now().plusDays(1))
                .availableTo(LocalDate.now().plusMonths(2))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .conditionDetail("사용감이 적습니다.")
                .build());
    }

    private EquipmentImage saveImage(
            Equipment equipment,
            String imageUrl,
            int sortOrder,
            boolean thumbnail
    ) {
        return equipmentImageRepository.saveAndFlush(EquipmentImage.builder()
                .equipment(equipment)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
