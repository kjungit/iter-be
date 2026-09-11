package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.RentalReview;

import java.time.LocalDateTime;

public record RentalReviewResponse(
        Long id,
        Long rentalId,
        Long reviewerId,
        Long revieweeId,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static RentalReviewResponse from(RentalReview review) {
        return new RentalReviewResponse(
                review.getId(),
                review.getRentalId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
