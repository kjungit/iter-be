package com.example.iter.auth.controller.api;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserAddress;
import com.example.iter.auth.domain.repository.UserAddressRepository;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserAddressApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userAddressRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticationIsRequired() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/address"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void missingDefaultAddressReturnsNotFound() throws Exception {
        User user = saveUser("address-missing@example.com");

        mockMvc.perform(get("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void putCreatesDefaultAddressWhenItDoesNotExist() throws Exception {
        User user = saveUser("address-create@example.com");

        mockMvc.perform(put("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("홍길동", "101동 202호")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.recipientName").value("홍길동"))
                .andExpect(jsonPath("$.recipientPhone").value("010-1234-5678"))
                .andExpect(jsonPath("$.zipcode").value("12345"))
                .andExpect(jsonPath("$.address").value("서울특별시 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.detailAddress").value("101동 202호"));

        UserAddress saved = userAddressRepository.findByUserIdAndDefaultAddressTrue(user.getId()).orElseThrow();
        assertThat(saved.isDefaultAddress()).isTrue();
    }

    @Test
    void putUpdatesExistingDefaultAddressWithoutCreatingAnotherOne() throws Exception {
        User user = saveUser("address-update@example.com");
        UserAddress existing = userAddressRepository.saveAndFlush(address(user.getId()));

        mockMvc.perform(put("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("김수정", "303호")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.recipientName").value("김수정"))
                .andExpect(jsonPath("$.detailAddress").value("303호"));

        assertThat(userAddressRepository.count()).isEqualTo(1);
    }

    @Test
    void getReturnsOnlyAuthenticatedUsersDefaultAddress() throws Exception {
        User currentUser = saveUser("address-owner@example.com");
        User otherUser = saveUser("address-other@example.com");
        UserAddress ownAddress = userAddressRepository.saveAndFlush(address(currentUser.getId()));
        userAddressRepository.saveAndFlush(address(otherUser.getId()));

        mockMvc.perform(get("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownAddress.getId()))
                .andExpect(jsonPath("$.recipientName").value("기존수령인"));
    }

    @Test
    void blankDetailAddressIsRejected() throws Exception {
        User user = saveUser("address-blank-detail@example.com");

        mockMvc.perform(put("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("홍길동", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("상세 주소는 필수입니다."));

        assertThat(userAddressRepository.count()).isZero();
    }

    @Test
    void invalidPhoneNumberIsRejected() throws Exception {
        User user = saveUser("address-invalid-phone@example.com");

        mockMvc.perform(put("/api/v1/users/me/address")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName":"홍길동",
                                  "recipientPhone":"02-1234-5678",
                                  "zipcode":"12345",
                                  "address":"서울특별시 강남구 테헤란로 123",
                                  "detailAddress":"101동 202호"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("올바른 전화번호 형식이 아닙니다."));

        assertThat(userAddressRepository.count()).isZero();
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .name("테스트회원")
                .nickname("테스터")
                .phone("010-0000-0000")
                .build());
    }

    private UserAddress address(Long userId) {
        return UserAddress.builder()
                .userId(userId)
                .recipientName("기존수령인")
                .recipientPhone("010-1111-2222")
                .zipcode("54321")
                .address("서울특별시 종로구")
                .detailAddress("202호")
                .defaultAddress(true)
                .build();
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    private String validRequest(String recipientName, String detailAddress) {
        return """
                {
                  "recipientName":"%s",
                  "recipientPhone":"010-1234-5678",
                  "zipcode":"12345",
                  "address":"서울특별시 강남구 테헤란로 123",
                  "detailAddress":"%s"
                }
                """.formatted(recipientName, detailAddress);
    }
}
