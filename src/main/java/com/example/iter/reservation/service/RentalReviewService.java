package com.example.iter.reservation.service;

import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalReview;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.domain.repository.RentalReviewRepository;
import com.example.iter.reservation.dto.request.RentalReviewCreateRequest;
import com.example.iter.reservation.dto.response.RentalReviewResponse;
import com.example.iter.reservation.dto.response.UserReviewStatsResponse;
import com.example.iter.reservation.event.RentalReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalReviewService {

    private final RentalReviewRepository rentalReviewRepository;
    private final RentalRepository rentalRepository;
    private final EquipmentRepository equipmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 거래 당사자(대여자 또는 장비 등록자)가 상대방에게 리뷰를 남깁니다.
    @Transactional
    public RentalReviewResponse createReview(Long reviewerId, Long rentalId, RentalReviewCreateRequest request) {
        Rental rental = findRental(rentalId);
        Equipment equipment = findEquipment(rental.getEquipmentId());

        Long revieweeId = resolveRevieweeId(reviewerId, rental, equipment);

        RentalReview saved = rentalReviewRepository.save(
                RentalReview.builder()
                        .rentalId(rental.getId())
                        .reviewerId(reviewerId)
                        .revieweeId(revieweeId)
                        .rating(request.rating())
                        .content(request.content().trim())
                        .build()
        );

        eventPublisher.publishEvent(new RentalReviewCreatedEvent(saved.getId()));

        return RentalReviewResponse.from(saved);
    }

    // 거래 당사자가 해당 거래에 달린 리뷰(최대 2건, 대여자/등록자 각 1건)를 조회합니다.
    @Transactional(readOnly = true)
    public List<RentalReviewResponse> getReviewsForRental(Long userId, Long rentalId) {
        Rental rental = findRental(rentalId);
        Equipment equipment = findEquipment(rental.getEquipmentId());
        validateParty(userId, rental, equipment);

        return rentalReviewRepository.findAllByRentalId(rentalId).stream()
                .map(RentalReviewResponse::from)
                .toList();
    }

    // 특정 사용자가 받은 리뷰 목록을 커서(keyset) 방식으로 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<RentalReviewResponse> getReviewsForUser(Long userId, String cursor, int size) {
        CursorKey cursorKey = CursorCodec.decode(cursor);
        LocalDateTime cursorCreatedAt = cursorKey == null ? null : cursorKey.createdAt();
        Long cursorId = cursorKey == null ? null : cursorKey.id();

        Pageable limit = PageRequest.of(0, size + 1);
        List<RentalReview> reviews = rentalReviewRepository.findNextByRevieweeId(
                userId, cursorCreatedAt, cursorId, limit);

        return CursorPageResponse.from(
                reviews,
                size,
                RentalReviewResponse::from,
                review -> new CursorKey(review.getCreatedAt(), review.getId())
        );
    }

    // 특정 사용자가 받은 리뷰의 평균 평점과 개수를 조회합니다.
    @Transactional(readOnly = true)
    public UserReviewStatsResponse getReviewStats(Long userId) {
        return UserReviewStatsResponse.from(rentalReviewRepository.findRatingStatsByRevieweeId(userId));
    }

    // 특정 사용자가 작성한 리뷰 목록을 커서(keyset) 방식으로 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<RentalReviewResponse> getReviewsWrittenByUser(Long userId, String cursor, int size) {
        CursorKey cursorKey = CursorCodec.decode(cursor);
        LocalDateTime cursorCreatedAt = cursorKey == null ? null : cursorKey.createdAt();
        Long cursorId = cursorKey == null ? null : cursorKey.id();

        Pageable limit = PageRequest.of(0, size + 1);
        List<RentalReview> reviews = rentalReviewRepository.findNextByReviewerId(
                userId, cursorCreatedAt, cursorId, limit);

        return CursorPageResponse.from(
                reviews,
                size,
                RentalReviewResponse::from,
                review -> new CursorKey(review.getCreatedAt(), review.getId())
        );
    }

    // ===================================================

    private Rental findRental(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new CustomException(ErrorCode.RENTAL_NOT_FOUND));
    }

    private Equipment findEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private void validateParty(Long userId, Rental rental, Equipment equipment) {
        boolean renter = rental.isRenter(userId);
        boolean owner = equipment.isOwnedBy(userId);

        if (!renter && !owner) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    // 로그인 사용자가 이 거래의 대여자인지 등록자인지 판별해 리뷰 대상(revieweeId)을 정하고,
    // 리뷰 작성이 가능한 상태인지(반납 완료 여부, 중복 작성 여부) 검증합니다.
    private Long resolveRevieweeId(Long reviewerId, Rental rental, Equipment equipment) {
        Long revieweeId;
        if (rental.isRenter(reviewerId)) {
            revieweeId = equipment.getOwnerId();
        } else if (equipment.isOwnedBy(reviewerId)) {
            revieweeId = rental.getRenterId();
        } else {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (rental.getStatus() != RentalStatus.COMPLETED) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ALLOWED_STATUS);
        }

        if (rentalReviewRepository.existsByRentalIdAndReviewerId(rental.getId(), reviewerId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        return revieweeId;
    }
}
