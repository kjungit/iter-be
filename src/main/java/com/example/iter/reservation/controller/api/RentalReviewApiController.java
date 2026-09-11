package com.example.iter.reservation.controller.api;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.reservation.controller.api.spec.RentalReviewApiSpec;
import com.example.iter.reservation.dto.request.RentalReviewCreateRequest;
import com.example.iter.reservation.dto.response.RentalReviewResponse;
import com.example.iter.reservation.dto.response.UserReviewStatsResponse;
import com.example.iter.reservation.service.RentalReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class RentalReviewApiController implements RentalReviewApiSpec {

    private final RentalReviewService rentalReviewService;

    @Override
    @PostMapping("/rentals/{rentalId}/reviews")
    public ResponseEntity<RentalReviewResponse> createReview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId,
            @Valid @RequestBody RentalReviewCreateRequest request
    ) {
        Long reviewerId = principal.getUser().getId();
        return ResponseEntity.ok(rentalReviewService.createReview(reviewerId, rentalId, request));
    }

    @Override
    @GetMapping("/rentals/{rentalId}/reviews")
    public ResponseEntity<List<RentalReviewResponse>> getReviewsForRental(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long rentalId
    ) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(rentalReviewService.getReviewsForRental(userId, rentalId));
    }

    @Override
    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<CursorPageResponse<RentalReviewResponse>> getReviewsForUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(rentalReviewService.getReviewsForUser(userId, cursor, size));
    }

    @Override
    @GetMapping("/users/{userId}/reviews/stats")
    public ResponseEntity<UserReviewStatsResponse> getReviewStats(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(rentalReviewService.getReviewStats(userId));
    }

    @Override
    @GetMapping("/users/{userId}/reviews/written")
    public ResponseEntity<CursorPageResponse<RentalReviewResponse>> getReviewsWrittenByUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(rentalReviewService.getReviewsWrittenByUser(userId, cursor, size));
    }
}
