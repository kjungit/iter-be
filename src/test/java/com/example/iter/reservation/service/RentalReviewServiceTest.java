package com.example.iter.reservation.service;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentCategory;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.reservation.domain.entity.Rental;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.repository.RentalRepository;
import com.example.iter.reservation.domain.repository.RentalReviewRepository;
import com.example.iter.reservation.dto.request.RentalReviewCreateRequest;
import com.example.iter.reservation.dto.response.RentalReviewResponse;
import com.example.iter.reservation.event.RentalReviewCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalReviewServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long RENTER_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;
    private static final Long RENTAL_ID = 10L;
    private static final Long EQUIPMENT_ID = 20L;

    @Mock
    private RentalReviewRepository rentalReviewRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RentalReviewService rentalReviewService;

    @Test
    void 반납_완료_상태가_아니면_리뷰_작성시_CONFLICT() {
        Rental rental = rental(RentalStatus.RETURNED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        assertThatThrownBy(() -> rentalReviewService.createReview(RENTER_ID, RENTAL_ID, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED_STATUS);

        verify(rentalReviewRepository, never()).save(any());
    }

    @Test
    void 이미_작성한_거래에_재작성시_CONFLICT() {
        Rental rental = rental(RentalStatus.COMPLETED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(rentalReviewRepository.existsByRentalIdAndReviewerId(RENTAL_ID, RENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> rentalReviewService.createReview(RENTER_ID, RENTAL_ID, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);

        verify(rentalReviewRepository, never()).save(any());
    }

    @Test
    void 거래_당사자가_아니면_FORBIDDEN() {
        Rental rental = rental(RentalStatus.COMPLETED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));

        assertThatThrownBy(() -> rentalReviewService.createReview(OUTSIDER_ID, RENTAL_ID, request()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(rentalReviewRepository, never()).save(any());
    }

    @Test
    void 대여자가_작성하면_대상은_장비등록자다() {
        Rental rental = rental(RentalStatus.COMPLETED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(rentalReviewRepository.existsByRentalIdAndReviewerId(RENTAL_ID, RENTER_ID)).thenReturn(false);
        when(rentalReviewRepository.save(any())).thenAnswer(invocation -> {
            var review = invocation.getArgument(0, com.example.iter.reservation.domain.entity.RentalReview.class);
            return com.example.iter.reservation.domain.entity.RentalReview.builder()
                    .id(100L)
                    .rentalId(review.getRentalId())
                    .reviewerId(review.getReviewerId())
                    .revieweeId(review.getRevieweeId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .build();
        });

        RentalReviewResponse response = rentalReviewService.createReview(RENTER_ID, RENTAL_ID, request());

        assertThat(response.reviewerId()).isEqualTo(RENTER_ID);
        assertThat(response.revieweeId()).isEqualTo(OWNER_ID);

        ArgumentCaptor<RentalReviewCreatedEvent> eventCaptor = ArgumentCaptor.forClass(RentalReviewCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reviewId()).isEqualTo(100L);
    }

    @Test
    void 등록자가_작성하면_대상은_대여자다() {
        Rental rental = rental(RentalStatus.COMPLETED);
        when(rentalRepository.findById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment()));
        when(rentalReviewRepository.existsByRentalIdAndReviewerId(RENTAL_ID, OWNER_ID)).thenReturn(false);
        when(rentalReviewRepository.save(any())).thenAnswer(invocation -> {
            var review = invocation.getArgument(0, com.example.iter.reservation.domain.entity.RentalReview.class);
            return com.example.iter.reservation.domain.entity.RentalReview.builder()
                    .id(101L)
                    .rentalId(review.getRentalId())
                    .reviewerId(review.getReviewerId())
                    .revieweeId(review.getRevieweeId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .build();
        });

        RentalReviewResponse response = rentalReviewService.createReview(OWNER_ID, RENTAL_ID, request());

        assertThat(response.reviewerId()).isEqualTo(OWNER_ID);
        assertThat(response.revieweeId()).isEqualTo(RENTER_ID);
    }

    private RentalReviewCreateRequest request() {
        return new RentalReviewCreateRequest(5, "정말 좋은 거래였습니다.");
    }

    private Rental rental(RentalStatus status) {
        return Rental.builder()
                .id(RENTAL_ID)
                .equipmentId(EQUIPMENT_ID)
                .renterId(RENTER_ID)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 10))
                .productNameSnapshot("예약 당시 맥북")
                .categorySnapshot("노트북")
                .dailyPriceSnapshot(BigDecimal.valueOf(30000))
                .rentalDays(10)
                .totalPrice(BigDecimal.valueOf(300000))
                .status(status)
                .build();
    }

    private Equipment equipment() {
        return Equipment.builder()
                .id(EQUIPMENT_ID)
                .ownerId(OWNER_ID)
                .category(EquipmentCategory.LAPTOP)
                .name("현재 장비명")
                .dailyPrice(BigDecimal.valueOf(50000))
                .build();
    }
}
