package com.example.iter.device.controller.api;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
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

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentScheduleApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 소유자는_조회_기간과_겹치는_확정_예약_일정만_조회한다() throws Exception {
        User owner = saveUser("schedule-owner@example.com", Role.USER);
        Equipment equipment = saveEquipment(owner);
        LocalDate from = LocalDate.now().plusDays(10);
        LocalDate to = LocalDate.now().plusDays(20);
        Rental approved = saveRental(
                equipment, RentalStatus.APPROVED, from.minusDays(2), from.plusDays(1));
        Rental disputed = saveRental(
                equipment, RentalStatus.DISPUTED, to.minusDays(1), to.plusDays(2));
        saveRental(equipment, RentalStatus.REQUESTED, from.plusDays(3), from.plusDays(4));
        saveRental(equipment, RentalStatus.COMPLETED, from.plusDays(5), from.plusDays(6));
        saveRental(equipment, RentalStatus.APPROVED, to.plusDays(1), to.plusDays(3));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", equipment.getId())
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(equipment.getId()))
                .andExpect(jsonPath("$.from").value(from.toString()))
                .andExpect(jsonPath("$.to").value(to.toString()))
                .andExpect(jsonPath("$.rentals.length()").value(2))
                .andExpect(jsonPath("$.rentals[0].rentalId").value(approved.getId()))
                .andExpect(jsonPath("$.rentals[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.rentals[1].rentalId").value(disputed.getId()))
                .andExpect(jsonPath("$.rentals[1].status").value("DISPUTED"))
                .andExpect(jsonPath("$.rentals[0].renterId").doesNotExist());
    }

    @Test
    void 관리자는_다른_회원_장비의_예약_일정을_조회할_수_있다() throws Exception {
        User owner = saveUser("schedule-admin-owner@example.com", Role.USER);
        User admin = saveUser("schedule-admin@example.com", Role.ADMIN);
        Equipment equipment = saveEquipment(owner);
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(10);
        saveRental(equipment, RentalStatus.RENTING, from, from.plusDays(2));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", equipment.getId())
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentals.length()").value(1));
    }

    @Test
    void 일반_회원은_다른_회원_장비의_예약_일정을_조회할_수_없다() throws Exception {
        User owner = saveUser("schedule-private-owner@example.com", Role.USER);
        User other = saveUser("schedule-private-other@example.com", Role.USER);
        Equipment equipment = saveEquipment(owner);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", equipment.getId())
                        .queryParam("from", LocalDate.now().toString())
                        .queryParam("to", LocalDate.now().plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 조회_기간이_누락되거나_역전되면_400을_반환한다() throws Exception {
        User owner = saveUser("schedule-invalid-period@example.com", Role.USER);
        Equipment equipment = saveEquipment(owner);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", equipment.getId())
                        .queryParam("from", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", equipment.getId())
                        .queryParam("from", LocalDate.now().plusDays(2).toString())
                        .queryParam("to", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 존재하지_않는_장비는_404를_반환한다() throws Exception {
        User owner = saveUser("schedule-not-found@example.com", Role.USER);

        mockMvc.perform(get("/api/v1/devices/{equipmentId}/rentals", Long.MAX_VALUE)
                        .queryParam("from", LocalDate.now().toString())
                        .queryParam("to", LocalDate.now().plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
    }

    private User saveUser(String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .name("일정 조회자")
                .nickname("조회자")
                .phone("010-1111-2222")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Equipment saveEquipment(User owner) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(owner.getId())
                .category(EquipmentCategory.CAMERA)
                .name("예약 장비")
                .description("예약 일정 테스트 장비")
                .dailyPrice(BigDecimal.valueOf(30_000))
                .availableFrom(LocalDate.now())
                .availableTo(LocalDate.now().plusMonths(3))
                .status(EquipmentStatus.ACTIVE)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private Rental saveRental(
            Equipment equipment,
            RentalStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(equipment.getId())
                .renterId(999L)
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

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
