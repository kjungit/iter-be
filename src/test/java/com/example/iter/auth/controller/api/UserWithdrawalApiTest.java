package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.RefreshToken;
import com.example.iter.auth.domain.entity.OAuthAccount;
import com.example.iter.auth.domain.entity.OAuthProvider;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.OAuthAccountRepository;
import com.example.iter.auth.domain.repository.RefreshTokenRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.service.RefreshTokenService;
import com.example.iter.common.security.JwtTokenProvider;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserWithdrawalApiTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 비밀번호_회원이_탈퇴하면_회원과_장비를_소프트_삭제하고_인증정보를_폐기한다() throws Exception {
        User user = saveUser(true);
        Equipment equipment = saveEquipment(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = refreshTokenService.issueForLogin(user);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(new Cookie("iter-refresh", rawRefreshToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("iter-refresh=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        User withdrawn = userRepository.findById(user.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(withdrawn.getDeletedAt()).isNotNull();
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.DELETED);
        assertThat(refreshTokenRepository.findAllByUserId(user.getId()))
                .hasSize(1)
                .allMatch(RefreshToken::isRevoked);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호_회원은_현재_비밀번호가_없거나_틀리면_탈퇴할_수_없다() throws Exception {
        User user = saveUser(true);
        Equipment equipment = saveEquipment(user.getId());
        refreshTokenService.issueForLogin(user);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest("WrongPassword!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        assertWithdrawalDidNotOccur(user, equipment);
    }

    @Test
    void OAuth_전용_회원은_비밀번호_없이_탈퇴할_수_있다() throws Exception {
        User user = saveUser(false);
        oAuthAccountRepository.saveAndFlush(OAuthAccount.builder()
                .userId(user.getId())
                .provider(OAuthProvider.KAKAO)
                .providerUserId("withdraw-kakao-" + System.nanoTime())
                .build());

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isNoContent());

        User withdrawn = userRepository.findById(user.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(withdrawn.getDeletedAt()).isNotNull();
        assertThat(oAuthAccountRepository.existsByUserIdAndProvider(user.getId(), OAuthProvider.KAKAO))
                .isTrue();
    }

    @Test
    void 대여자로서_진행_중인_거래가_있으면_탈퇴할_수_없다() throws Exception {
        User renter = saveUser(true);
        User owner = saveUser(true);
        Equipment equipment = saveEquipment(owner.getId());
        saveRental(equipment.getId(), renter.getId(), RentalStatus.RENTING);

        assertActiveRentalBlocksWithdrawal(renter);
    }

    @Test
    void 장비_소유자로서_진행_중인_거래가_있으면_탈퇴할_수_없다() throws Exception {
        User owner = saveUser(true);
        User renter = saveUser(true);
        Equipment equipment = saveEquipment(owner.getId());
        saveRental(equipment.getId(), renter.getId(), RentalStatus.DISPUTED);

        assertActiveRentalBlocksWithdrawal(owner);
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.ACTIVE);
    }

    @Test
    void 완료_거절_취소된_거래만_있으면_탈퇴할_수_있다() throws Exception {
        User target = saveUser(true);
        User other = saveUser(true);
        Equipment targetEquipment = saveEquipment(target.getId());
        Equipment otherEquipment = saveEquipment(other.getId());

        saveRental(targetEquipment.getId(), other.getId(), RentalStatus.COMPLETED);
        saveRental(otherEquipment.getId(), target.getId(), RentalStatus.REJECTED);
        saveRental(otherEquipment.getId(), target.getId(), RentalStatus.CANCELED);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(target))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(PASSWORD)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.DELETED);
    }

    @Test
    void 정지_회원도_진행_중인_거래가_없으면_탈퇴할_수_있다() throws Exception {
        User user = saveUser(true);
        user.suspend();
        userRepository.flush();

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(PASSWORD)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.DELETED);
    }

    @Test
    void 인증되지_않은_회원은_탈퇴할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    private void assertActiveRentalBlocksWithdrawal(User user) throws Exception {
        refreshTokenService.issueForLogin(user);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequest(PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_RENTAL_EXISTS"));

        User unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(unchanged.getDeletedAt()).isNull();
        assertThat(refreshTokenRepository.findAllByUserId(user.getId()))
                .allMatch(token -> !token.isRevoked());
    }

    private void assertWithdrawalDidNotOccur(User user, Equipment equipment) {
        User unchanged = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(unchanged.getDeletedAt()).isNull();
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(refreshTokenRepository.findAllByUserId(user.getId()))
                .allMatch(token -> !token.isRevoked());
    }

    private User saveUser(boolean withPassword) {
        return userRepository.saveAndFlush(User.builder()
                .email("withdraw-" + System.nanoTime() + "@example.com")
                .password(withPassword ? passwordEncoder.encode(PASSWORD) : null)
                .name("탈퇴회원")
                .nickname("withdraw-user")
                .phone("010-1111-2222")
                .build());
    }

    private Equipment saveEquipment(Long ownerId) {
        return equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(ownerId)
                .category(EquipmentCategory.CAMERA)
                .name("테스트 장비")
                .description("탈퇴 테스트용 장비")
                .dailyPrice(BigDecimal.valueOf(10_000))
                .availableFrom(LocalDate.now().plusDays(1))
                .availableTo(LocalDate.now().plusDays(30))
                .status(EquipmentStatus.ACTIVE)
                .productCondition(ProductConditionType.NORMAL)
                .build());
    }

    private Rental saveRental(Long equipmentId, Long renterId, RentalStatus status) {
        return rentalRepository.saveAndFlush(Rental.builder()
                .equipmentId(equipmentId)
                .renterId(renterId)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .productNameSnapshot("테스트 장비")
                .categorySnapshot("CAMERA")
                .dailyPriceSnapshot(BigDecimal.valueOf(10_000))
                .rentalDays(3)
                .totalPrice(BigDecimal.valueOf(30_000))
                .status(status)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    private String passwordRequest(String password) {
        return """
                {
                  "password": "%s"
                }
                """.formatted(password);
    }
}
