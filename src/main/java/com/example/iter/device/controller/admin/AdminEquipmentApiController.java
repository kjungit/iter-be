package com.example.iter.device.controller.admin;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.controller.admin.spec.AdminEquipmentApiSpec;
import com.example.iter.device.dto.request.AdminEquipmentSearchRequest;
import com.example.iter.device.dto.request.AdminEquipmentStatusRequest;
import com.example.iter.device.dto.response.AdminEquipmentDetailResponse;
import com.example.iter.device.dto.response.AdminEquipmentSummaryResponse;
import com.example.iter.device.service.AdminEquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/equipment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEquipmentApiController implements AdminEquipmentApiSpec {

    private final AdminEquipmentService adminEquipmentService;

    // 관리자가 검색 조건과 페이지 정보로 전체 장비 목록을 조회합니다.
    @Override
    @GetMapping
    public ResponseEntity<PageResponse<AdminEquipmentSummaryResponse>> getEquipments(@ModelAttribute AdminEquipmentSearchRequest request) {
        return ResponseEntity.ok(adminEquipmentService.getEquipments(request));
    }

    // 관리자가 특정 장비의 등록자, 상태, 이미지 등 상세 정보를 조회합니다.
    @Override
    @GetMapping("/{equipmentId}")
    public ResponseEntity<AdminEquipmentDetailResponse> getEquipmentDetail(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(adminEquipmentService.getEquipmentDetail(equipmentId));
    }

    // 관리자가 장비를 차단하거나 차단을 해제하고 처리 이력을 저장합니다.
    @Override
    @PatchMapping("/{equipmentId}/status")
    public ResponseEntity<AdminEquipmentDetailResponse> updateEquipmentStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @RequestBody AdminEquipmentStatusRequest request
    ) {
        Long adminId = principal.getUser().getId();

        return ResponseEntity.ok(adminEquipmentService.updateEquipmentStatus(adminId, equipmentId, request));
    }
}
