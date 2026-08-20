package com.example.iter.device.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 장비_관리_API가_OpenAPI_문서에_노출된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{equipmentId}/images'].post")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{equipmentId}/images'].post.responses['201']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{equipmentId}/images/{imageId}'].delete")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{equipmentId}/images/{imageId}'].delete.responses['204']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/devices'].get")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{equipmentId}/rentals'].get")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices'].post.summary")
                        .value("장비 등록"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.summary")
                        .value("로그인"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/csrf'].get.parameters")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/oauth2/kakao/exchange'].post.parameters[0].name")
                        .value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.summary")
                        .value("내 정보 조회"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me/payments'].get.summary")
                        .value("내 결제 내역 조회"))
                .andExpect(jsonPath("$.components.schemas.EquipmentImageCreateRequest")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.EquipmentScheduleResponse")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.MyEquipmentSummaryResponse")
                        .exists());
    }
}
