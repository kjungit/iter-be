package com.example.iter.reservation.controller.api;

import com.example.iter.reservation.dto.request.EvidenceImagePresignRequest;
import com.example.iter.reservation.dto.response.EvidenceImagePresignResponse;
import com.example.iter.reservation.service.RentalEvidenceUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental Fulfillment", description = "수령/반납 증빙 사진 업로드용 presigned URL 발급")
@RestController
@RequestMapping("/api/v1/rentals/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class RentalEvidenceUploadApiController {

    private final RentalEvidenceUploadService rentalEvidenceUploadService;

    @Operation(summary = "증빙 사진 업로드용 presigned URL 발급", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/presigned-urls")
    public ResponseEntity<EvidenceImagePresignResponse> createPresignedUploads(
            @Valid @RequestBody EvidenceImagePresignRequest request
    ) {
        return ResponseEntity.ok(rentalEvidenceUploadService.createPresignedUploads(request));
    }
}
