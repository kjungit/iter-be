package com.example.iter.device.service;

import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.dto.request.EquipmentSearchRequest;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentImageResponse;
import com.example.iter.device.dto.response.EquipmentListResponse;
import com.example.iter.device.dto.response.EquipmentOwnerResponse;
import com.example.iter.device.dto.response.EquipmentSummaryResponse;
import com.example.iter.device.service.model.EquipmentSearchRow;
import com.example.iter.reservation.domain.policy.RentalConflictPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EquipmentDetailResponse getEquipmentDetail(Long equipmentId) {
        var row = equipmentRepository.findPublicDetailById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        var equipment = row.equipment();
        var owner = userRepository.findSummaryById(equipment.getOwnerId())
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        List<EquipmentImageResponse> images = equipmentImageRepository
                .findByEquipmentIdOrderBySortOrderAscIdAsc(equipmentId)
                .stream()
                .map(EquipmentImageResponse::from)
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

    public EquipmentListResponse getEquipmentList(EquipmentSearchRequest request) {
        Page<EquipmentSearchRow> rows = equipmentRepository.searchPublicEquipment(
                escapeLikePattern(request.keyword()),
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.startDate(),
                request.endDate(),
                RentalConflictPolicy.nonConfirmedStatuses(),
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
                        image.getEquipment().getId(), image.getImageUrl()));
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
