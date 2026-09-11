package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.RentalReview;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RentalReviewRepositoryTest {

    private static final Long REVIEWEE_ID = 100L;
    private static final Long OTHER_REVIEWEE_ID = 200L;
    private static final Long REVIEWER_ID = 300L;

    @Autowired
    private RentalReviewRepository rentalReviewRepository;

    @Test
    void 평균_평점과_리뷰_개수는_실제_저장된_평점을_집계한_값이다() {
        rentalReviewRepository.saveAndFlush(review(1L, REVIEWER_ID, REVIEWEE_ID, 5));
        rentalReviewRepository.saveAndFlush(review(2L, REVIEWER_ID, REVIEWEE_ID, 3));
        rentalReviewRepository.saveAndFlush(review(3L, REVIEWER_ID, REVIEWEE_ID, 4));
        // 다른 사용자 리뷰는 집계에 섞이면 안 됨
        rentalReviewRepository.saveAndFlush(review(4L, REVIEWER_ID, OTHER_REVIEWEE_ID, 1));

        RentalReviewRepository.RatingStats stats =
                rentalReviewRepository.findRatingStatsByRevieweeId(REVIEWEE_ID);

        assertThat(stats.getReviewCount()).isEqualTo(3L);
        assertThat(stats.getAverageRating()).isCloseTo(4.0, within(0.001)); // (5+3+4)/3
    }

    @Test
    void 리뷰가_하나도_없으면_개수는_0이고_평균은_null이다() {
        RentalReviewRepository.RatingStats stats =
                rentalReviewRepository.findRatingStatsByRevieweeId(999L);

        assertThat(stats.getReviewCount()).isEqualTo(0L);
        assertThat(stats.getAverageRating()).isNull();
    }

    @Test
    void 작성한_리뷰_목록은_reviewerId로만_걸러진다() {
        rentalReviewRepository.saveAndFlush(review(10L, REVIEWER_ID, REVIEWEE_ID, 5));
        rentalReviewRepository.saveAndFlush(review(11L, REVIEWER_ID, OTHER_REVIEWEE_ID, 2));
        rentalReviewRepository.saveAndFlush(review(12L, 999L, REVIEWEE_ID, 1)); // 다른 작성자

        var written = rentalReviewRepository.findNextByReviewerId(
                REVIEWER_ID, null, null, PageRequest.of(0, 10));

        assertThat(written).hasSize(2);
        assertThat(written).allMatch(r -> r.getReviewerId().equals(REVIEWER_ID));
    }

    private RentalReview review(Long rentalId, Long reviewerId, Long revieweeId, int rating) {
        return RentalReview.builder()
                .rentalId(rentalId)
                .reviewerId(reviewerId)
                .revieweeId(revieweeId)
                .rating(rating)
                .content("테스트 리뷰")
                .build();
    }
}
