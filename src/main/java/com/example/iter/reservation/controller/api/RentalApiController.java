package com.example.iter.reservation.controller.api;

import com.example.iter.auth.domain.entity.Role;
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
import com.example.iter.reservation.controller.api.spec.RentalApiSpec;
import com.example.iter.reservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalApiController implements RentalApiSpec {

    private final RentalService rentalService;

    @Override
    @PostMapping
    public ResponseEntity<RentalCreateResponse> createRental(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody RentalCreateRequest request
    ) {
        RentalCreateResponse response = rentalService.createRental(principal.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalDetailResponse> getRentalDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId
    ) {
        RentalDetailResponse response = rentalService.getRentalDetail(
                rentalId, principal.getUser().getId(), principal.getUser().getRole() == Role.ADMIN);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(params = "role=owner")
    public ResponseEntity<PageResponse<RentalReceivedItemResponse>> getReceivedRentals(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) RentalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<RentalReceivedItemResponse> response = rentalService.getReceivedRentals(
                principal.getUser().getId(), status, page, size);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{rentalId}/cancel")
    public ResponseEntity<RentalCancelResponse> cancelRental(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId
    ) {
        RentalCancelResponse response = rentalService.cancelRental(
                rentalId, principal.getUser().getId(), principal.getUser().getRole() == Role.ADMIN);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{rentalId}/approve")
    public ResponseEntity<RentalApproveResponse> approveRental(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId
    ) {
        RentalApproveResponse response = rentalService.approveRental(
                rentalId, principal.getUser().getId(), principal.getUser().getRole() == Role.ADMIN);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{rentalId}/reject")
    public ResponseEntity<RentalRejectResponse> rejectRental(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId,
            @RequestBody RentalRejectRequest request
    ) {
        RentalRejectResponse response = rentalService.rejectRental(
                rentalId, principal.getUser().getId(), principal.getUser().getRole() == Role.ADMIN, request.reason());
        return ResponseEntity.ok(response);
    }
}
