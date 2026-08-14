package com.example.iter.reservation.controller.api;

import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
import com.example.iter.reservation.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Return", description = "반납 확인 API")
@Validated
@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "JWT")
public class ReturnApiController {

    private final ReturnService returnService;


    // 등록자가 확인해야 하는 반납 거래 목록을 조회합니다.
    @Operation(summary = "반납 확인 대상 목록 조회")
    @GetMapping("/returns")
    public ResponseEntity<PageResponse<ReturnTargetResponse>>
    getReturnTargets(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute PagingRequest request
    ) {
        Long ownerId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.getReturnTargets(ownerId, request));
    }


    // 수령 당시 증빙과 반납 당시 증빙을 비교 조회합니다.
    @Operation(summary = "수령·반납 증빙 비교")
    @GetMapping("/{rentalId}/return-comparison")
    public ResponseEntity<ReturnComparisonResponse>
    getReturnComparison(
            @AuthenticationPrincipal CustomUserDetails principal,

            @PathVariable
            @Positive(message = "대여 ID는 1 이상이어야 합니다.")
            Long rentalId
    ) {
        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.getReturnComparison(userId, rentalId));
    }


    // 등록자가 반납을 정상 또는 비정상으로 최종 확인합니다.
    @Operation(summary = "반납 최종 확인")
    @PostMapping("/{rentalId}/return-confirmation")
    public ResponseEntity<ReturnConfirmationResponse> confirmReturn(
            @AuthenticationPrincipal CustomUserDetails principal,

            @PathVariable
            @Positive(message = "대여 ID는 1 이상이어야 합니다.")
            Long rentalId,

            @Valid @RequestBody ReturnConfirmationRequest request
    ) {
        Long ownerId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.confirmReturn(ownerId, rentalId, request));
    }
}
