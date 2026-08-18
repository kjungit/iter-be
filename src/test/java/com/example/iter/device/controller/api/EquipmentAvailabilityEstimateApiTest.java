package com.example.iter.device.controller.api;

import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
class EquipmentAvailabilityEstimateApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    void 내일부터_이틀_이상인_기간은_대여_가능하다() throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(1);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/availability", equipment.getId())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(equipment.getId()))
                .andExpect(jsonPath("$.startDate").value(startDate.toString()))
                .andExpect(jsonPath("$.endDate").value(endDate.toString()))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void 기본_대여_기간을_벗어나면_이유와_함께_대여_불가를_반환한다() throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(29);
        LocalDate endDate = LocalDate.now().plusDays(31);

        mockMvc.perform(availabilityRequest(equipment, startDate, endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reason").value("OUT_OF_AVAILABLE_PERIOD"));
    }

    @ParameterizedTest
    @EnumSource(value = RentalStatus.class, names = {
            "PENDING", "REQUESTED", "APPROVED", "SHIPPING", "RECEIVED", "RENTING",
            "RETURN_REQUESTED", "RETURNING", "RETURNED", "DISPUTED"
    })
    void 점유_예약과_겹치면_대여_불가를_반환한다(RentalStatus status) throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        saveRental(equipment, startDate, endDate, status);

        mockMvc.perform(availabilityRequest(equipment, startDate, endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reason").value("RESERVATION_CONFLICT"));
    }

    @ParameterizedTest
    @EnumSource(value = RentalStatus.class, names = {
            "REJECTED", "CANCELED", "COMPLETED"
    })
    void 비점유_예약은_대여_가능_여부를_막지_않는다(RentalStatus status) throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        saveRental(equipment, startDate, endDate, status);

        mockMvc.perform(availabilityRequest(equipment, startDate, endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void 시작일과_종료일을_포함해_예상_대여_금액을_계산한다() throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(5);

        mockMvc.perform(estimateRequest(equipment, startDate, endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(equipment.getId()))
                .andExpect(jsonPath("$.startDate").value(startDate.toString()))
                .andExpect(jsonPath("$.endDate").value(endDate.toString()))
                .andExpect(jsonPath("$.rentalDays").value(6))
                .andExpect(jsonPath("$.dailyPrice").value(30000))
                .andExpect(jsonPath("$.totalPrice").value(180000));
    }

    @Test
    void 대여할_수_없는_기간의_예상_금액은_409를_반환한다() throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        saveRental(equipment, startDate, endDate, RentalStatus.APPROVED);

        mockMvc.perform(estimateRequest(equipment, startDate, endDate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("선택한 기간에는 장비를 대여할 수 없습니다."));

        mockMvc.perform(estimateRequest(
                        equipment,
                        LocalDate.now().plusDays(29),
                        LocalDate.now().plusDays(31)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_AVAILABLE"));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void 공개_상태가_아닌_장비는_두_API에서_404를_반환한다(EquipmentStatus status) throws Exception {
        Equipment equipment = saveEquipment(status);
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(1);

        mockMvc.perform(availabilityRequest(equipment, startDate, endDate))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));

        mockMvc.perform(estimateRequest(equipment, startDate, endDate))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void 존재하지_않는_장비는_두_API에서_404를_반환한다() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(1);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/availability", Long.MAX_VALUE)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/estimate", Long.MAX_VALUE)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void 누락_오늘_당일대여_역전된_날짜는_400을_반환한다() throws Exception {
        Equipment equipment = saveEquipment(EquipmentStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/availability", equipment.getId())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("대여 시작일과 종료일을 올바르게 입력해주세요."));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/estimate", equipment.getId())
                        .param("startDate", today.toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/estimate", equipment.getId())
                        .param("startDate", today.plusDays(1).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/estimate", equipment.getId())
                        .param("startDate", today.plusDays(2).toString())
                        .param("endDate", today.plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private MockHttpServletRequestBuilder availabilityRequest(
            Equipment equipment,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return get("/api/v1/devices/{equipmentId}/availability", equipment.getId())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString());
    }

    private MockHttpServletRequestBuilder estimateRequest(
            Equipment equipment,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return get("/api/v1/devices/{equipmentId}/estimate", equipment.getId())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString());
    }

    private Equipment saveEquipment(EquipmentStatus status) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(1L)
                .category(EquipmentCategory.CAMERA)
                .name("소니 A7C2")
                .description("대여 가능 여부 테스트 장비")
                .dailyPrice(BigDecimal.valueOf(30_000))
                .availableFrom(LocalDate.now())
                .availableTo(LocalDate.now().plusDays(30))
                .status(status)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private void saveRental(
            Equipment equipment,
            LocalDate startDate,
            LocalDate endDate,
            RentalStatus status
    ) {
        rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(99L)
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
