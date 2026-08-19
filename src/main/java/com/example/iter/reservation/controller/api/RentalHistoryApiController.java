package com.example.iter.reservation.controller.api;

import com.example.iter.common.dto.request.PagingRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.controller.api.spec.RentalHistoryApiSpec;
import com.example.iter.reservation.dto.request.RentalHistorySearchRequest;
import com.example.iter.reservation.dto.response.RentalHistoryResponse;
import com.example.iter.reservation.service.RentalHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalHistoryApiController implements RentalHistoryApiSpec {

    private final RentalHistoryService rentalHistoryService;

    // 로그인 사용자가 빌린 장비 이력을 조회합니다.
    @Override
    @GetMapping("/borrowed")
    public ResponseEntity<PageResponse<RentalHistoryResponse>> getBorrowedHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute RentalHistorySearchRequest request
    ) {
        return ResponseEntity.ok(rentalHistoryService.getBorrowedHistory(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 빌려준 장비 이력을 조회합니다.
    @Override
    @GetMapping("/lent")
    public ResponseEntity<PageResponse<RentalHistoryResponse>> getLentHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute RentalHistorySearchRequest request
    ) {
        return ResponseEntity.ok(rentalHistoryService.getLentHistory(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 빌린 장비 중 현재 연체 중인 거래를 조회합니다.
    @Override
    @GetMapping("/borrowed/overdue")
    public ResponseEntity<PageResponse<RentalHistoryResponse>> getBorrowedOverdueHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute PagingRequest request
    ) {
        return ResponseEntity.ok(rentalHistoryService.getBorrowedOverdueHistory(principal.getUser().getId(), request));
    }

    // 로그인 사용자가 빌려준 장비 중 현재 연체 중인 거래를 조회합니다.
    @Override
    @GetMapping("/lent/overdue")
    public ResponseEntity<PageResponse<RentalHistoryResponse>> getLentOverdueHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute PagingRequest request
    ) {
        return ResponseEntity.ok(rentalHistoryService.getLentOverdueHistory(principal.getUser().getId(), request));
    }
}
