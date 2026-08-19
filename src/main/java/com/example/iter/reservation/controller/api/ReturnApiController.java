package com.example.iter.reservation.controller.api;

import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.controller.api.spec.ReturnApiSpec;
import com.example.iter.reservation.dto.request.ReturnConfirmationRequest;
import com.example.iter.reservation.dto.response.ReturnComparisonResponse;
import com.example.iter.reservation.dto.response.ReturnConfirmationResponse;
import com.example.iter.reservation.dto.response.ReturnTargetResponse;
import com.example.iter.reservation.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ReturnApiController implements ReturnApiSpec {

    private final ReturnService returnService;

    // 등록자가 확인해야 하는 반납 거래 목록을 조회합니다.
    @Override
    @GetMapping("/returns")
    public ResponseEntity<PageResponse<ReturnTargetResponse>> getReturnTargets(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute PagingRequest request
    ) {
        Long ownerId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.getReturnTargets(ownerId, request));
    }

    // 수령 당시 증빙과 반납 당시 증빙을 비교 조회합니다.
    @Override
    @GetMapping("/{rentalId}/return-comparison")
    public ResponseEntity<ReturnComparisonResponse> getReturnComparison(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId
    ) {
        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.getReturnComparison(userId, rentalId));
    }


    // 등록자가 반납을 정상 또는 비정상으로 최종 확인합니다.
    @Override
    @PostMapping("/{rentalId}/return-confirmation")
    public ResponseEntity<ReturnConfirmationResponse> confirmReturn(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId,
            @RequestBody ReturnConfirmationRequest request
    ) {
        Long ownerId = principal.getUser().getId();

        return ResponseEntity.ok(returnService.confirmReturn(ownerId, rentalId, request));
    }
}
