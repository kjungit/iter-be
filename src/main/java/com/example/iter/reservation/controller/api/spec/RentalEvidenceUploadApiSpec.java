package com.example.iter.reservation.controller.api.spec;

import com.example.iter.reservation.dto.request.EvidenceImagePresignRequest;
import com.example.iter.reservation.dto.response.EvidenceImagePresignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Rental Fulfillment", description = "수령/반납 증빙 사진 업로드용 presigned URL 발급")
@SecurityRequirement(name = "JWT")
public interface RentalEvidenceUploadApiSpec {

    @Operation(summary = "증빙 사진 업로드용 presigned URL 발급")
    ResponseEntity<EvidenceImagePresignResponse> createPresignedUploads(
            @Valid EvidenceImagePresignRequest request
    );
}
