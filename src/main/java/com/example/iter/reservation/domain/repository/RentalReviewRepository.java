package com.example.iter.reservation.domain.repository;

import com.example.iter.reservation.domain.entity.RentalReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RentalReviewRepository extends JpaRepository<RentalReview, Long> {

    boolean existsByRentalIdAndReviewerId(Long rentalId, Long reviewerId);

    List<RentalReview> findAllByRentalId(Long rentalId);

    // offset 대신 keyset(cursor) 방식 — NotificationRepository.findNextByReceiverId와 동일한 패턴.
    @Query("""
            select r
            from RentalReview r
            where r.revieweeId = :revieweeId
              and (
                    :cursorCreatedAt is null
                    or r.createdAt < :cursorCreatedAt
                    or (r.createdAt = :cursorCreatedAt and r.id < :cursorId)
                  )
            order by r.createdAt desc, r.id desc
            """)
    List<RentalReview> findNextByRevieweeId(
            @Param("revieweeId") Long revieweeId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            select avg(r.rating) as averageRating, count(r) as reviewCount
            from RentalReview r
            where r.revieweeId = :revieweeId
            """)
    RatingStats findRatingStatsByRevieweeId(@Param("revieweeId") Long revieweeId);

    interface RatingStats {
        Double getAverageRating();

        Long getReviewCount();
    }
}
