package com.example.iter.reservation.controller.api.spec;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.dto.request.RentalCreateRequest;
import com.example.iter.reservation.dto.request.RentalRejectRequest;
import com.example.iter.reservation.dto.response.RentalApproveResponse;
import com.example.iter.reservation.dto.response.RentalCancelResponse;
import com.example.iter.reservation.dto.response.RentalCreateResponse;
import com.example.iter.reservation.dto.response.RentalDetailResponse;
import com.example.iter.reservation.dto.response.RentalReceivedItemResponse;
import com.example.iter.reservation.dto.response.RentalRejectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Rental", description = "대여 요청 API")
@SecurityRequirement(name = "JWT")
public interface RentalApiSpec {

    @Operation(summary = "대여 요청 생성")
    ResponseEntity<RentalCreateResponse> createRental(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Valid RentalCreateRequest request
    );

    @Operation(summary = "대여 요청 상세 조회 (+ 연체 여부)")
    ResponseEntity<RentalDetailResponse> getRentalDetail(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "대여 거래 ID", example = "101", required = true) Long rentalId
    );

    @Operation(summary = "받은 요청 목록")
    ResponseEntity<PageResponse<RentalReceivedItemResponse>> getReceivedRentals(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "필터링할 대여 상태") RentalStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지 크기") int size
    );

    @Operation(summary = "승인 전 예약 취소")
    ResponseEntity<RentalCancelResponse> cancelRental(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "대여 거래 ID", example = "101", required = true) Long rentalId
    );

    @Operation(summary = "대여 요청 승인 (비관적 락)")
    ResponseEntity<RentalApproveResponse> approveRental(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "대여 거래 ID", example = "101", required = true) Long rentalId
    );

    @Operation(summary = "대여 요청 거절")
    ResponseEntity<RentalRejectResponse> rejectRental(
            @Parameter(hidden = true) CustomUserDetails principal,
            @Parameter(description = "대여 거래 ID", example = "101", required = true) Long rentalId,
            @Valid RentalRejectRequest request
    );
}
