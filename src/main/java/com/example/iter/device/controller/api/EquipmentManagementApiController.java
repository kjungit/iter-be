package com.example.iter.device.controller.api;

import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.controller.api.spec.EquipmentManagementApiSpec;
import com.example.iter.device.dto.request.EquipmentCreateRequest;
import com.example.iter.device.dto.request.EquipmentImageCreateRequest;
import com.example.iter.device.dto.request.EquipmentStatusUpdateRequest;
import com.example.iter.device.dto.request.EquipmentScheduleRequest;
import com.example.iter.device.dto.request.EquipmentUpdateRequest;
import com.example.iter.device.dto.request.PresignedImageUploadRequest;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentImageResponse;
import com.example.iter.device.dto.response.EquipmentStatusResponse;
import com.example.iter.device.dto.response.EquipmentScheduleResponse;
import com.example.iter.device.dto.response.PresignedImageUploadResponse;
import com.example.iter.device.service.EquipmentImageUploadService;
import com.example.iter.device.service.EquipmentManagementService;
import com.example.iter.device.service.EquipmentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
public class EquipmentManagementApiController implements EquipmentManagementApiSpec {

    private final EquipmentManagementService equipmentManagementService;
    private final EquipmentImageUploadService equipmentImageUploadService;
    private final EquipmentQueryService equipmentQueryService;

    @Override
    @PostMapping("/images/presigned-urls")
    public ResponseEntity<PresignedImageUploadResponse> issueImageUploadUrls(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PresignedImageUploadRequest request
    ) {
        return ResponseEntity.ok(equipmentImageUploadService.issue(
                principal.getUser(), request));
    }

    @Override
    @PostMapping
    public ResponseEntity<EquipmentDetailResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EquipmentCreateRequest request
    ) {
        EquipmentDetailResponse response = equipmentManagementService.create(
                principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PatchMapping("/{equipmentId}")
    public ResponseEntity<EquipmentDetailResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentManagementService.update(
                principal.getUser().getId(), equipmentId, request));
    }

    @Override
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId
    ) {
        equipmentManagementService.delete(principal.getUser().getId(), equipmentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{equipmentId}/status")
    public ResponseEntity<EquipmentStatusResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentManagementService.updateStatus(
                principal.getUser(), equipmentId, request));
    }

    @Override
    @PostMapping("/{equipmentId}/images")
    public ResponseEntity<List<EquipmentImageResponse>> addImages(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentImageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                equipmentManagementService.addImages(
                        principal.getUser(), equipmentId, request));
    }

    @Override
    @DeleteMapping("/{equipmentId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @PathVariable Long imageId
    ) {
        equipmentManagementService.deleteImage(
                principal.getUser().getId(), equipmentId, imageId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{equipmentId}/rentals")
    public ResponseEntity<EquipmentScheduleResponse> getSchedule(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @ModelAttribute EquipmentScheduleRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getEquipmentSchedule(
                principal.getUser(), equipmentId, request));
    }
}
