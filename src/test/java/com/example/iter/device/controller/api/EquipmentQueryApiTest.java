package com.example.iter.device.controller.api;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class EquipmentQueryApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private EquipmentImageRepository equipmentImageRepository;
    @Autowired
    private RentalRepository rentalRepository;

    @BeforeEach
    void setUp() {
        equipmentImageRepository.deleteAll();
        rentalRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    void 공개_장비만_최신순으로_조회하고_페이지_메타데이터를_반환한다() throws Exception {
        Equipment oldEquipment = saveEquipment("소니 A7C2", EquipmentCategory.CAMERA, 30_000,
                EquipmentStatus.ACTIVE);
        Equipment latestEquipment = saveEquipment("맥북 프로", EquipmentCategory.LAPTOP, 50_000,
                EquipmentStatus.ACTIVE);
        saveEquipment("비공개 장비", EquipmentCategory.CAMERA, 10_000, EquipmentStatus.INACTIVE);
        saveThumbnail(latestEquipment, "https://example.com/macbook.jpg");

        mockMvc.perform(get("/api/v1/devices").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(latestEquipment.getId()))
                .andExpect(jsonPath("$.content[0].thumbnailUrl").value("https://example.com/macbook.jpg"))
                .andExpect(jsonPath("$.content[0].averageRating").value(0.0))
                .andExpect(jsonPath("$.content[0].reviewCount").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void 검색_가격_카테고리_기간을_필터링하고_선점_예약과_겹치는_장비는_제외한다() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(3);
        Equipment available = saveEquipment("소니 카메라", EquipmentCategory.CAMERA, 30_000,
                EquipmentStatus.ACTIVE);
        Equipment booked = saveEquipment("소니 예약 카메라", EquipmentCategory.CAMERA, 40_000,
                EquipmentStatus.ACTIVE);
        Equipment requested = saveEquipment("소니 승인대기 카메라", EquipmentCategory.CAMERA, 45_000,
                EquipmentStatus.ACTIVE);
        saveRental(booked, startDate, endDate, RentalStatus.APPROVED, 1L);
        saveRental(requested, startDate, endDate, RentalStatus.REQUESTED, 2L);

        mockMvc.perform(get("/api/v1/devices")
                        .param("keyword", "소니")
                        .param("category", "CAMERA")
                        .param("minPrice", "20000")
                        .param("maxPrice", "45000")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(available.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void 평점순_요청은_리뷰_비활성화로_최신순과_기본값을_반환한다() throws Exception {
        saveEquipment("이전 장비", EquipmentCategory.CAMERA, 10_000, EquipmentStatus.ACTIVE);
        Equipment latest = saveEquipment("최신 장비", EquipmentCategory.CAMERA, 20_000,
                EquipmentStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/devices").param("sort", "RATING_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(latest.getId()))
                .andExpect(jsonPath("$.content[0].averageRating").value(0.0))
                .andExpect(jsonPath("$.content[0].reviewCount").value(0));
    }

    @Test
    void 가격순은_ID를_보조_정렬로_사용하고_리뷰_기본값을_반환한다() throws Exception {
        Equipment samePriceLowerId = saveEquipment("동일 가격 A", EquipmentCategory.CAMERA, 10_000,
                EquipmentStatus.ACTIVE);
        Equipment samePriceHigherId = saveEquipment("동일 가격 B", EquipmentCategory.CAMERA, 10_000,
                EquipmentStatus.ACTIVE);
        Equipment expensive = saveEquipment("고가 장비", EquipmentCategory.CAMERA, 20_000,
                EquipmentStatus.ACTIVE);
        mockMvc.perform(get("/api/v1/devices").param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(samePriceHigherId.getId()))
                .andExpect(jsonPath("$.content[0].averageRating").value(0.0))
                .andExpect(jsonPath("$.content[0].reviewCount").value(0))
                .andExpect(jsonPath("$.content[1].id").value(samePriceLowerId.getId()))
                .andExpect(jsonPath("$.content[2].id").value(expensive.getId()));

        mockMvc.perform(get("/api/v1/devices").param("sort", "PRICE_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expensive.getId()))
                .andExpect(jsonPath("$.content[1].id").value(samePriceHigherId.getId()))
                .andExpect(jsonPath("$.content[2].id").value(samePriceLowerId.getId()));
    }

    @Test
    void 결과가_없으면_빈_페이지를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/devices").param("keyword", "존재하지않음"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void 검색어의_LIKE_특수문자는_와일드카드가_아닌_문자_그대로_검색한다() throws Exception {
        Equipment percent = saveEquipment("할인율 100% 카메라", EquipmentCategory.CAMERA, 10_000,
                EquipmentStatus.ACTIVE);
        Equipment underscore = saveEquipment("제품_A 카메라", EquipmentCategory.CAMERA, 20_000,
                EquipmentStatus.ACTIVE);
        Equipment backslash = saveEquipment("경로\\카메라", EquipmentCategory.CAMERA, 25_000,
                EquipmentStatus.ACTIVE);
        saveEquipment("일반 카메라", EquipmentCategory.CAMERA, 30_000, EquipmentStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/devices").param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(percent.getId()));

        mockMvc.perform(get("/api/v1/devices").param("keyword", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(underscore.getId()));

        mockMvc.perform(get("/api/v1/devices").param("keyword", "\\"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(backslash.getId()));
    }

    @Test
    void 과거_오늘_동일한_시작종료일은_검색조건으로_허용하지_않는다() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/devices")
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/devices")
                        .param("startDate", today.toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        LocalDate futureDate = today.plusDays(1);
        mockMvc.perform(get("/api/v1/devices")
                        .param("startDate", futureDate.toString())
                        .param("endDate", futureDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 잘못된_검색조건과_enum은_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/devices")
                        .param("minPrice", "50000")
                        .param("maxPrice", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("최소 가격은 최대 가격보다 클 수 없습니다."));

        mockMvc.perform(get("/api/v1/devices")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("대여 시작일은 오늘 이후이고 종료일은 시작일 이후여야 합니다."));

        mockMvc.perform(get("/api/v1/devices").param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Equipment saveEquipment(
            String name,
            EquipmentCategory category,
            long dailyPrice,
            EquipmentStatus status
    ) {
        return equipmentRepository.save(Equipment.builder()
                .ownerId(1L)
                .category(category)
                .name(name)
                .description(name + " 설명")
                .dailyPrice(BigDecimal.valueOf(dailyPrice))
                .availableFrom(LocalDate.now())
                .availableTo(LocalDate.now().plusMonths(2))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private void saveThumbnail(Equipment equipment, String imageUrl) {
        equipmentImageRepository.save(EquipmentImage.builder()
                .equipment(equipment)
                .imageUrl(imageUrl)
                .sortOrder(0)
                .thumbnail(true)
                .build());
    }

    private void saveRental(
            Equipment equipment,
            LocalDate startDate,
            LocalDate endDate,
            RentalStatus status,
            Long renterId
    ) {
        rentalRepository.save(Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(renterId)
                .startDate(startDate)
                .endDate(endDate)
                .productNameSnapshot(equipment.getName())
                .categorySnapshot(equipment.getCategory().name())
                .dailyPriceSnapshot(equipment.getDailyPrice())
                .rentalDays((int) (endDate.toEpochDay() - startDate.toEpochDay() + 1))
                .totalPrice(equipment.getDailyPrice())
                .status(status)
                .build());
    }
}
