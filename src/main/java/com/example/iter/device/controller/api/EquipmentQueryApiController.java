package com.example.iter.device.controller.api;

import com.example.iter.device.dto.request.EquipmentSearchRequest;
import com.example.iter.device.dto.response.EquipmentListResponse;
import com.example.iter.device.service.EquipmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Equipment", description = "장비 조회 API")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class EquipmentQueryApiController {

    private final EquipmentQueryService equipmentQueryService;

    @Operation(summary = "장비 목록 조회", description = "공개 중인 장비를 검색 조건과 정렬 기준으로 조회합니다.")
    @GetMapping
    public ResponseEntity<EquipmentListResponse> getEquipmentList(
            @Valid @ModelAttribute EquipmentSearchRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentList(request));
    }
}
