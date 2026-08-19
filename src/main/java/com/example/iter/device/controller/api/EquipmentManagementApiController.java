package com.example.iter.device.controller.api;

import com.example.iter.common.security.CustomUserDetails;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Equipment Management", description = "장비 등록·수정·삭제·공개 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
@SecurityRequirement(name = "JWT")
public class EquipmentManagementApiController {

    private final EquipmentManagementService equipmentManagementService;
    private final EquipmentImageUploadService equipmentImageUploadService;
    private final EquipmentQueryService equipmentQueryService;

    @Operation(summary = "장비 이미지 업로드 URL 발급",
            description = "S3에 이미지를 직접 업로드할 수 있는 5분 만료 Presigned PUT URL을 발급합니다.")
    @PostMapping("/images/presigned-urls")
    public ResponseEntity<PresignedImageUploadResponse> issueImageUploadUrls(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PresignedImageUploadRequest request
    ) {
        return ResponseEntity.ok(equipmentImageUploadService.issue(
                principal.getUser(), request));
    }

    @Operation(summary = "장비 등록",
            description = "Presigned URL로 업로드한 이미지 객체 키와 장비 정보를 등록합니다.")
    @ApiResponse(responseCode = "201", description = "장비 등록 성공")
    @PostMapping
    public ResponseEntity<EquipmentDetailResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EquipmentCreateRequest request
    ) {
        EquipmentDetailResponse response = equipmentManagementService.create(
                principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "장비 수정")
    @PatchMapping("/{equipmentId}")
    public ResponseEntity<EquipmentDetailResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentManagementService.update(
                principal.getUser().getId(), equipmentId, request));
    }

    @Operation(summary = "장비 삭제", description = "진행 중 거래와 분쟁이 없는 장비를 소프트 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "장비 삭제 성공")
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId
    ) {
        equipmentManagementService.delete(principal.getUser().getId(), equipmentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "장비 공개 중지 또는 재개")
    @PatchMapping("/{equipmentId}/status")
    public ResponseEntity<EquipmentStatusResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentManagementService.updateStatus(
                principal.getUser(), equipmentId, request));
    }

    @Operation(summary = "장비 이미지 등록",
            description = "Presigned URL로 업로드한 이미지를 기존 장비에 추가합니다. 기존 이미지와 합쳐 최대 5장까지 등록할 수 있습니다.")
    @ApiResponse(responseCode = "201", description = "장비 이미지 등록 성공")
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

    @Operation(summary = "장비 이미지 삭제",
            description = "장비에는 최소 한 장의 이미지를 유지합니다. 대표 이미지를 삭제하면 다음 순서의 이미지가 대표 이미지가 됩니다.")
    @ApiResponse(responseCode = "204", description = "장비 이미지 삭제 성공")
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

    @Operation(summary = "장비 예약 일정 조회",
            description = "장비 소유자 또는 관리자가 조회 기간과 겹치는 확정 예약 일정을 조회합니다.")
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
