package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.repository.RentalReviewRepository;

public record UserReviewStatsResponse(
        double averageRating,
        long reviewCount
) {
    public static UserReviewStatsResponse from(RentalReviewRepository.RatingStats stats) {
        if (stats == null || stats.getReviewCount() == null || stats.getReviewCount() == 0) {
            return new UserReviewStatsResponse(0.0, 0L);
        }
        return new UserReviewStatsResponse(stats.getAverageRating(), stats.getReviewCount());
    }
}
