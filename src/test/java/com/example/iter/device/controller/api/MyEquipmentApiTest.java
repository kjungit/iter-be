package com.example.iter.device.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.endsWith;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MyEquipmentApiTest {

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

    @Test
    void 내_장비만_상태별로_필터링하고_가격순으로_조회한다() throws Exception {
        User owner = saveUser("my-equipment-owner@example.com");
        User other = saveUser("my-equipment-other@example.com");
        Equipment expensive = saveEquipment(owner, "고가 비공개 장비", 50_000, EquipmentStatus.INACTIVE);
        Equipment cheap = saveEquipment(owner, "저가 비공개 장비", 20_000, EquipmentStatus.INACTIVE);
        saveEquipment(owner, "공개 장비", 10_000, EquipmentStatus.ACTIVE);
        saveEquipment(other, "다른 회원 장비", 1_000, EquipmentStatus.INACTIVE);
        saveThumbnail(cheap, "equipment/public/%d/thumbnail.jpg".formatted(cheap.getId()));

        mockMvc.perform(get("/api/v1/users/me/devices")
                        .queryParam("status", "INACTIVE")
                        .queryParam("sort", "PRICE_ASC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(cheap.getId()))
                .andExpect(jsonPath("$.content[0].thumbnailUrl")
                        .value(endsWith("equipment/public/%d/thumbnail.jpg".formatted(cheap.getId()))))
                .andExpect(jsonPath("$.content[1].id").value(expensive.getId()))
                .andExpect(jsonPath("$.content[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void 상태를_생략하면_삭제된_장비를_포함한_모든_내_장비를_조회한다() throws Exception {
        User owner = saveUser("all-status-owner@example.com");
        Equipment active = saveEquipment(owner, "활성 장비", 10_000, EquipmentStatus.ACTIVE);
        Equipment deleted = saveEquipment(owner, "삭제 장비", 20_000, EquipmentStatus.DELETED);

        mockMvc.perform(get("/api/v1/users/me/devices")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(deleted.getId()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void 내_장비_평점순_요청은_리뷰_비활성화로_최신순을_반환한다() throws Exception {
        User owner = saveUser("rating-sort-owner@example.com");
        saveEquipment(owner, "이전 장비", 10_000, EquipmentStatus.ACTIVE);
        Equipment latest = saveEquipment(owner, "최신 장비", 20_000, EquipmentStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/users/me/devices")
                        .queryParam("sort", "RATING_DESC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(latest.getId()));
    }

    @Test
    void 잘못된_상태와_페이지_조건은_400을_반환한다() throws Exception {
        User owner = saveUser("invalid-my-search@example.com");

        mockMvc.perform(get("/api/v1/users/me/devices")
                        .queryParam("status", "UNKNOWN")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/users/me/devices")
                        .queryParam("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 인증하지_않으면_내_장비를_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/devices"))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .name("장비 소유자")
                .nickname("장비주인")
                .phone("010-1234-5678")
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Equipment saveEquipment(
            User owner,
            String name,
            long dailyPrice,
            EquipmentStatus status
    ) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(owner.getId())
                .category(EquipmentCategory.CAMERA)
                .name(name)
                .description("장비 설명")
                .dailyPrice(BigDecimal.valueOf(dailyPrice))
                .availableFrom(LocalDate.now())
                .availableTo(LocalDate.now().plusMonths(1))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private void saveThumbnail(Equipment equipment, String objectKey) {
        equipmentImageRepository.saveAndFlush(EquipmentImage.builder()
                .equipment(equipment)
                .imageUrl("https://cdn.example.com/" + objectKey)
                .objectKey(objectKey)
                .sortOrder(0)
                .thumbnail(true)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
