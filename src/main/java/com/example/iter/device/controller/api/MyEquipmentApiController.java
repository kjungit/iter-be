package com.example.iter.device.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.dto.request.MyEquipmentSearchRequest;
import com.example.iter.device.dto.response.MyEquipmentSummaryResponse;
import com.example.iter.device.service.EquipmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Equipment", description = "내 장비 관리 API")
@RestController
@RequestMapping("/api/v1/users/me/devices")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class MyEquipmentApiController {

    private final EquipmentQueryService equipmentQueryService;

    @Operation(summary = "등록 장비 목록 및 상태별 조회",
            description = "인증 회원이 등록한 장비를 상태별로 필터링하고 정렬하여 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<MyEquipmentSummaryResponse>> getMyEquipment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute MyEquipmentSearchRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getMyEquipment(
                principal.getUser().getId(), request));
    }
}
