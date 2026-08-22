package com.example.iter.device.controller.api;

import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.controller.api.spec.EquipmentQueryApiSpec;
import com.example.iter.device.dto.request.EquipmentAvailabilityRequest;
import com.example.iter.device.dto.request.EquipmentEstimateRequest;
import com.example.iter.device.dto.request.EquipmentSearchRequest;
import com.example.iter.device.dto.response.EquipmentAvailabilityResponse;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentEstimateResponse;
import com.example.iter.device.dto.response.EquipmentListResponse;
import com.example.iter.device.service.EquipmentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class EquipmentQueryApiController implements EquipmentQueryApiSpec {

    private final EquipmentQueryService equipmentQueryService;

    @Override
    @GetMapping("/{equipmentId}/availability")
    public ResponseEntity<EquipmentAvailabilityResponse> getEquipmentAvailability(
            @PathVariable Long equipmentId,
            @Valid @ModelAttribute EquipmentAvailabilityRequest request
    ) {
        return ResponseEntity.ok(
                equipmentQueryService.getEquipmentAvailability(equipmentId, request));
    }

    @Override
    @GetMapping("/{equipmentId}/estimate")
    public ResponseEntity<EquipmentEstimateResponse> getEquipmentEstimate(
            @PathVariable Long equipmentId,
            @Valid @ModelAttribute EquipmentEstimateRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentEstimate(equipmentId, request));
    }

    @Override
    @GetMapping("/{equipmentId}")
    public ResponseEntity<EquipmentDetailResponse> getEquipmentDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId
    ) {
        Long requesterId = principal == null ? null : principal.getUser().getId();
        return ResponseEntity.ok(equipmentQueryService.getEquipmentDetail(requesterId, equipmentId));
    }

    @Override
    @GetMapping
    public ResponseEntity<EquipmentListResponse> getEquipmentList(
            @Valid @ModelAttribute EquipmentSearchRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentList(request));
    }
}
