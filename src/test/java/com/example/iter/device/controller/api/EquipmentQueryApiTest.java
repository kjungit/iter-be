package com.example.iter.device.controller.api;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.entity.Review;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.domain.repository.ReviewRepository;
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
    private ReviewRepository reviewRepository;
    @Autowired
    private RentalRepository rentalRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
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
    void 검색_가격_카테고리_기간을_필터링하고_확정_예약과_겹치는_장비는_제외한다() throws Exception {
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
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(available.getId()))
                .andExpect(jsonPath("$.content[1].id").value(requested.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void 평점순은_평균평점_리뷰수_ID순으로_정렬한다() throws Exception {
        Equipment lower = saveEquipment("평점 4점", EquipmentCategory.CAMERA, 10_000,
                EquipmentStatus.ACTIVE);
        Equipment higher = saveEquipment("평점 5점", EquipmentCategory.CAMERA, 20_000,
                EquipmentStatus.ACTIVE);
        saveReview(lower, 4, 10L);
        saveReview(higher, 5, 11L);
        saveReview(higher, 5, 12L);

        mockMvc.perform(get("/api/v1/devices").param("sort", "RATING_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(higher.getId()))
                .andExpect(jsonPath("$.content[0].averageRating").value(5.0))
                .andExpect(jsonPath("$.content[0].reviewCount").value(2))
                .andExpect(jsonPath("$.content[1].id").value(lower.getId()));
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
                .andExpect(jsonPath("$.message").value("대여 시작일과 종료일을 함께 올바르게 입력해주세요."));

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

    private void saveReview(Equipment equipment, int rating, long rentalId) {
        reviewRepository.save(Review.builder()
                .rentalId(rentalId)
                .equipment(equipment)
                .userId(rentalId)
                .rating(rating)
                .content("리뷰")
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
