package com.example.iter.device.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.dto.request.EquipmentAvailabilityRequest;
import com.example.iter.device.dto.request.EquipmentEstimateRequest;
import com.example.iter.device.dto.request.EquipmentSearchRequest;
import com.example.iter.device.dto.request.EquipmentSort;
import com.example.iter.device.dto.request.MyEquipmentSearchRequest;
import com.example.iter.device.dto.request.EquipmentScheduleRequest;
import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.device.dto.response.AvailabilityReason;
import com.example.iter.device.dto.response.EquipmentAvailabilityResponse;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentEstimateResponse;
import com.example.iter.device.dto.response.EquipmentImageResponse;
import com.example.iter.device.dto.response.EquipmentListResponse;
import com.example.iter.device.dto.response.EquipmentOwnerResponse;
import com.example.iter.device.dto.response.EquipmentSummaryResponse;
import com.example.iter.device.dto.response.MyEquipmentSummaryResponse;
import com.example.iter.device.dto.response.EquipmentScheduleResponse;
import com.example.iter.device.dto.response.RentalScheduleItemResponse;
import com.example.iter.device.support.EquipmentImageUrlResolver;
import com.example.iter.reservation.domain.policy.RentalConflictPolicy;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentQueryService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentImageRepository equipmentImageRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final EquipmentImageUrlResolver imageUrlResolver;

    public EquipmentAvailabilityResponse getEquipmentAvailability(
            Long equipmentId,
            EquipmentAvailabilityRequest request
    ) {
        Equipment equipment = findPublicEquipment(equipmentId);
        AvailabilityReason reason = findUnavailabilityReason(
                equipment, request.startDate(), request.endDate());

        return new EquipmentAvailabilityResponse(
                equipmentId,
                request.startDate(),
                request.endDate(),
                reason == null,
                reason
        );
    }

    public EquipmentEstimateResponse getEquipmentEstimate(
            Long equipmentId,
            EquipmentEstimateRequest request
    ) {
        Equipment equipment = findPublicEquipment(equipmentId);
        AvailabilityReason reason = findUnavailabilityReason(
                equipment, request.startDate(), request.endDate());
        if (reason != null) {
            throw new CustomException(ErrorCode.EQUIPMENT_RENTAL_PERIOD_UNAVAILABLE);
        }

        int rentalDays = Math.toIntExact(
                ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1);
        BigDecimal totalPrice = equipment.getDailyPrice().multiply(BigDecimal.valueOf(rentalDays));

        return new EquipmentEstimateResponse(
                equipmentId,
                request.startDate(),
                request.endDate(),
                rentalDays,
                equipment.getDailyPrice(),
                totalPrice
        );
    }

    // 공개(ACTIVE) 장비는 누구나 조회 가능하고, 그 외 상태(숨김/점검/차단/삭제)는 소유자 본인만 조회할 수 있다.
    public EquipmentDetailResponse getEquipmentDetail(Long requesterId, Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        boolean isOwner = requesterId != null && equipment.isOwnedBy(requesterId);
        if (equipment.getStatus() != EquipmentStatus.ACTIVE && !isOwner) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        var owner = userRepository.findSummaryById(equipment.getOwnerId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        List<EquipmentImageResponse> images = equipmentImageRepository
                .findByEquipmentIdOrderBySortOrderAscIdAsc(equipmentId)
                .stream()
                .map(image -> EquipmentImageResponse.from(
                        image, imageUrlResolver.resolve(image)))
                .toList();

        return new EquipmentDetailResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDescription(),
                equipment.getDailyPrice(),
                equipment.getAvailableFrom(),
                equipment.getAvailableTo(),
                equipment.getStatus(),
                equipment.getProductCondition(),
                equipment.getConditionDetail(),
                images,
                new EquipmentOwnerResponse(owner.userId(), owner.nickName()),
                0.0,
                0L,
                equipment.getCreatedAt()
        );
    }

    private Equipment findPublicEquipment(Long equipmentId) {
        return equipmentRepository.findByIdAndStatus(equipmentId, EquipmentStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private AvailabilityReason findUnavailabilityReason(
            Equipment equipment,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (equipment.getAvailableFrom() == null
                || equipment.getAvailableTo() == null
                || startDate.isBefore(equipment.getAvailableFrom())
                || endDate.isAfter(equipment.getAvailableTo())) {
            return AvailabilityReason.OUT_OF_AVAILABLE_PERIOD;
        }

        boolean conflict = rentalRepository.existsConflictingOccupyingRental(
                equipment.getId(),
                startDate,
                endDate,
                RentalConflictPolicy.nonOccupyingStatuses()
        );
        return conflict ? AvailabilityReason.RESERVATION_CONFLICT : null;
    }

    public EquipmentListResponse getEquipmentList(EquipmentSearchRequest request) {
        Page<Equipment> equipmentPage = findPublicEquipment(request);

        List<Long> equipmentIds = equipmentPage.getContent().stream()
                .map(Equipment::getId)
                .toList();
        Map<Long, String> thumbnailUrls = findThumbnailUrls(equipmentIds);
        List<EquipmentSummaryResponse> content = equipmentPage.getContent().stream()
                .map(equipment -> toResponse(
                        equipment,
                        thumbnailUrls.get(equipment.getId())
                ))
                .toList();

        return new EquipmentListResponse(
                content,
                equipmentPage.getNumber(),
                equipmentPage.getSize(),
                equipmentPage.getTotalElements(),
                equipmentPage.getTotalPages(),
                equipmentPage.isFirst(),
                equipmentPage.isLast()
        );
    }

    public PageResponse<MyEquipmentSummaryResponse> getMyEquipment(
            Long ownerId,
            MyEquipmentSearchRequest request
    ) {
        Page<Equipment> equipmentPage = findMyEquipment(ownerId, request);
        List<Long> equipmentIds = equipmentPage.getContent().stream()
                .map(Equipment::getId)
                .toList();
        Map<Long, String> thumbnailUrls = findThumbnailUrls(equipmentIds);
        Page<MyEquipmentSummaryResponse> responsePage = equipmentPage.map(equipment ->
                new MyEquipmentSummaryResponse(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getCategory(),
                    equipment.getDailyPrice(),
                    equipment.getStatus(),
                    equipment.getProductCondition(),
                    thumbnailUrls.get(equipment.getId()),
                    equipment.getAvailableFrom(),
                    equipment.getAvailableTo()
                )
        );
        return PageResponse.from(responsePage);
    }

    public EquipmentScheduleResponse getEquipmentSchedule(
            User requester,
            Long equipmentId,
            EquipmentScheduleRequest request
    ) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.EQUIPMENT_NOT_FOUND, "존재하지 않는 장비입니다."));
        if (!equipment.isOwnedBy(requester.getId()) && requester.getRole() != Role.ADMIN) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN, "본인 소유 장비의 예약 일정만 조회할 수 있습니다.");
        }

        List<RentalScheduleItemResponse> rentals = rentalRepository.findEquipmentSchedule(
                        equipmentId,
                        request.from(),
                        request.to(),
                        RentalConflictPolicy.nonScheduledStatuses())
                .stream()
                .map(RentalScheduleItemResponse::from)
                .toList();
        return new EquipmentScheduleResponse(
                equipmentId, request.from(), request.to(), rentals);
    }

    private Map<Long, String> findThumbnailUrls(List<Long> equipmentIds) {
        if (equipmentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> thumbnailUrls = new LinkedHashMap<>();
        equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(equipmentIds)
                .forEach(image -> thumbnailUrls.putIfAbsent(
                        image.getEquipment().getId(), imageUrlResolver.resolve(image)));
        return thumbnailUrls;
    }

    private Page<Equipment> findPublicEquipment(EquipmentSearchRequest request) {
        String keyword = escapeLikePattern(request.keyword());
        return equipmentRepository.searchPublicEquipment(
                keyword,
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.startDate(),
                request.endDate(),
                RentalConflictPolicy.nonOccupyingStatuses(),
                PageRequest.of(request.page(), request.size(), equipmentSort(request.sort()))
        );
    }

    private Page<Equipment> findMyEquipment(
            Long ownerId,
            MyEquipmentSearchRequest request
    ) {
        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                equipmentSort(request.sort())
        );
        return request.status() == null
                ? equipmentRepository.findByOwnerId(ownerId, pageable)
                : equipmentRepository.findByOwnerIdAndStatus(ownerId, request.status(), pageable);
    }

    private Sort equipmentSort(EquipmentSort sort) {
        return switch (sort) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case PRICE_ASC -> Sort.by(
                    Sort.Order.asc("dailyPrice"),
                    Sort.Order.desc("id")
            );
            case PRICE_DESC -> Sort.by(
                    Sort.Order.desc("dailyPrice"),
                    Sort.Order.desc("id")
            );
            // 리뷰 기능은 비활성화됐지만 기존 요청 계약을 유지하기 위해 최신순으로 처리합니다.
            case RATING_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }

    private EquipmentSummaryResponse toResponse(Equipment equipment, String thumbnailUrl) {
        return new EquipmentSummaryResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDailyPrice(),
                equipment.getAvailableFrom(),
                equipment.getAvailableTo(),
                equipment.getProductCondition(),
                thumbnailUrl,
                0.0,
                0L
        );
    }

    private String escapeLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
