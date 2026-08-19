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
import com.example.iter.device.service.model.EquipmentSearchRow;
import com.example.iter.device.support.EquipmentImageUrlResolver;
import com.example.iter.reservation.domain.policy.RentalConflictPolicy;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
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

    public EquipmentDetailResponse getEquipmentDetail(Long equipmentId) {
        var row = equipmentRepository.findPublicDetailById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        var equipment = row.equipment();
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
                row.averageRating(),
                row.reviewCount(),
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
        Page<EquipmentSearchRow> rows = equipmentRepository.searchPublicEquipment(
                request.keyword(),
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.startDate(),
                request.endDate(),
                RentalConflictPolicy.nonOccupyingStatuses(),
                request.sort().name(),
                PageRequest.of(request.page(), request.size())
        );

        Map<Long, String> thumbnailUrls = findThumbnailUrls(rows.getContent());
        List<EquipmentSummaryResponse> content = rows.getContent().stream()
                .map(row -> toResponse(row, thumbnailUrls.get(row.equipment().getId())))
                .toList();

        return new EquipmentListResponse(
                content,
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalElements(),
                rows.getTotalPages(),
                rows.isFirst(),
                rows.isLast()
        );
    }

    public PageResponse<MyEquipmentSummaryResponse> getMyEquipment(
            Long ownerId,
            MyEquipmentSearchRequest request
    ) {
        Page<EquipmentSearchRow> rows = equipmentRepository.searchMyEquipment(
                ownerId,
                request.status(),
                request.sort().name(),
                PageRequest.of(request.page(), request.size())
        );
        Map<Long, String> thumbnailUrls = findThumbnailUrls(rows.getContent());
        Page<MyEquipmentSummaryResponse> responsePage = rows.map(row -> {
            Equipment equipment = row.equipment();
            return new MyEquipmentSummaryResponse(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getCategory(),
                    equipment.getDailyPrice(),
                    equipment.getStatus(),
                    equipment.getProductCondition(),
                    thumbnailUrls.get(equipment.getId()),
                    equipment.getAvailableFrom(),
                    equipment.getAvailableTo()
            );
        });
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
                        RentalConflictPolicy.nonOccupyingStatuses())
                .stream()
                .map(RentalScheduleItemResponse::from)
                .toList();
        return new EquipmentScheduleResponse(
                equipmentId, request.from(), request.to(), rentals);
    }

    private Map<Long, String> findThumbnailUrls(List<EquipmentSearchRow> rows) {
        List<Long> equipmentIds = rows.stream()
                .map(row -> row.equipment().getId())
                .toList();

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

    private EquipmentSummaryResponse toResponse(EquipmentSearchRow row, String thumbnailUrl) {
        var equipment = row.equipment();
        return new EquipmentSummaryResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDailyPrice(),
                equipment.getAvailableFrom(),
                equipment.getAvailableTo(),
                equipment.getProductCondition(),
                thumbnailUrl,
                row.averageRating(),
                row.reviewCount()
        );
    }
}
