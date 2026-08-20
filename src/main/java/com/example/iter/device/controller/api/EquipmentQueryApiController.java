package com.example.iter.device.controller.api;

import com.example.iter.device.dto.request.EquipmentAvailabilityRequest;
import com.example.iter.device.dto.request.EquipmentEstimateRequest;
import com.example.iter.device.dto.request.EquipmentSearchRequest;
import com.example.iter.device.dto.response.EquipmentAvailabilityResponse;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentEstimateResponse;
import com.example.iter.device.dto.response.EquipmentListResponse;
import com.example.iter.device.service.EquipmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Equipment", description = "장비 조회 API")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class EquipmentQueryApiController {

    private final EquipmentQueryService equipmentQueryService;

    @Operation(summary = "대여 가능 여부/기간 확인", description = "장비의 기본 가능 기간과 기존 예약을 확인합니다.")
    @GetMapping("/{equipmentId}/availability")
    public ResponseEntity<EquipmentAvailabilityResponse> getEquipmentAvailability(
            @PathVariable Long equipmentId,
            @Valid @ModelAttribute EquipmentAvailabilityRequest request
    ) {
        return ResponseEntity.ok(
                equipmentQueryService.getEquipmentAvailability(equipmentId, request));
    }

    @Operation(summary = "예상 대여 금액 계산", description = "대여 가능한 기간의 일수와 예상 총액을 계산합니다.")
    @GetMapping("/{equipmentId}/estimate")
    public ResponseEntity<EquipmentEstimateResponse> getEquipmentEstimate(
            @PathVariable Long equipmentId,
            @Valid @ModelAttribute EquipmentEstimateRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentEstimate(equipmentId, request));
    }

    @Operation(summary = "장비 상세 정보 조회", description = "공개 중인 장비의 상세 정보와 이미지, 소유자, 평점을 조회합니다.")
    @GetMapping("/{equipmentId}")
    public ResponseEntity<EquipmentDetailResponse> getEquipmentDetail(
            @PathVariable Long equipmentId
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentDetail(equipmentId));
    }

    @Operation(summary = "장비 목록 조회", description = "공개 중인 장비를 검색 조건과 정렬 기준으로 조회합니다.")
    @GetMapping
    public ResponseEntity<EquipmentListResponse> getEquipmentList(
            @Valid @ModelAttribute EquipmentSearchRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentList(request));
    }
}
