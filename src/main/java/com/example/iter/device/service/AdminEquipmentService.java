package com.example.iter.device.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.audit.domain.entity.AdminActionTargetType;
import com.example.iter.common.audit.domain.entity.AdminActionType;
import com.example.iter.common.audit.service.AdminActionService;
import com.example.iter.common.dto.response.CursorPageResponse;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.common.pagination.CursorCodec;
import com.example.iter.common.pagination.CursorKey;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.dto.request.AdminEquipmentSearchRequest;
import com.example.iter.device.dto.request.AdminEquipmentStatusRequest;
import com.example.iter.device.dto.response.AdminEquipmentDetailResponse;
import com.example.iter.device.dto.response.AdminEquipmentSummaryResponse;
import com.example.iter.device.util.AdminEquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentImageRepository equipmentImageRepository;
    private final UserRepository userRepository;
    private final AdminActionService adminActionService;
    private final AdminEquipmentMapper adminEquipmentMapper;

    // 관리자가 장비명, 카테고리, 상태 조건으로 전체 장비 목록을 커서 조회합니다.
    @Transactional(readOnly = true)
    public CursorPageResponse<AdminEquipmentSummaryResponse> getEquipments(AdminEquipmentSearchRequest request) {
        String keyword = normalize(request.keyword());
        String category = normalize(request.category());
        CursorKey cursorKey = CursorCodec.decode(request.cursor());

        List<Equipment> equipment = equipmentRepository.searchForAdminByCursor(
                keyword,
                category,
                request.status(),
                cursorKey == null ? null : cursorKey.createdAt(),
                cursorKey == null ? null : cursorKey.id(),
                PageRequest.of(0, request.size() + 1)
        );

        Map<Long, User> ownerMap = loadOwners(equipment);
        Map<Long, String> thumbnailMap = loadThumbnails(equipment);

        return CursorPageResponse.from(
                equipment,
                request.size(),
                item -> adminEquipmentMapper.toSummary(
                        item,
                        getRequiredOwner(ownerMap, item.getOwnerId()),
                        thumbnailMap.get(item.getId())
                ),
                item -> new CursorKey(item.getCreatedAt(), item.getId())
        );
    }

    // 관리자가 특정 장비의 상세 정보와 전체 이미지를 조회합니다.
    @Transactional(readOnly = true)
    public AdminEquipmentDetailResponse getEquipmentDetail(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        User owner = userRepository.findById(equipment.getOwnerId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<EquipmentImage> images = equipmentImageRepository.findByEquipmentIdOrderBySortOrderAsc(equipmentId);

        return adminEquipmentMapper.toDetail(equipment, owner, images);
    }

    // 관리자가 장비를 차단 또는 차단 해제하고 관리자 조치 이력을 저장합니다.
    @Transactional
    public AdminEquipmentDetailResponse updateEquipmentStatus(
            Long adminId,
            Long equipmentId,
            AdminEquipmentStatusRequest request
    ) {
        Equipment equipment = equipmentRepository.findByIdForUpdate(equipmentId).orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));

        validateStatusChange(equipment, request.status());
        AdminActionType action = applyStatus(equipment, request.status());

        adminActionService.record(
                adminId,
                AdminActionTargetType.EQUIPMENT,
                equipment.getId(),
                action,
                request.reason().trim()
        );

        equipmentRepository.flush();

        User owner = userRepository.findById(equipment.getOwnerId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<EquipmentImage> images = equipmentImageRepository.findByEquipmentIdOrderBySortOrderAsc(equipmentId);

        return adminEquipmentMapper.toDetail(equipment, owner, images);
    }

    // 현재 상태와 요청 상태가 관리자의 장비 차단 정책에 맞는지 검증합니다.
    private void validateStatusChange(Equipment equipment, EquipmentStatus requestedStatus) {
        EquipmentStatus currentStatus = equipment.getStatus();

        // 관리자는 현재 장비 상태가 ACTIVE 또는 INACTIVE인 경우에만 장비 상태를 SUSPENDED로 변경하여 차단할 수 있습니다.
        boolean canSuspend = requestedStatus == EquipmentStatus.SUSPENDED
                && (currentStatus == EquipmentStatus.ACTIVE
                || currentStatus == EquipmentStatus.INACTIVE);

        // 차단 해제 시 바로 대여 가능한 상태가 되지 않도록 INACTIVE로 복구합니다.
        // 이후 장비를 다시 활성화할지는 등록자가 직접 결정합니다.
        boolean canRestore = requestedStatus == EquipmentStatus.INACTIVE && currentStatus == EquipmentStatus.SUSPENDED;

        if (!canSuspend && !canRestore) {
            throw new CustomException(ErrorCode.INVALID_EQUIPMENT_STATUS_TRANSITION);
        }
    }

    // 장비 상태를 변경하고 저장할 관리자 조치 유형을 반환합니다.
    private AdminActionType applyStatus(Equipment equipment, EquipmentStatus requestedStatus) {
        if (requestedStatus == EquipmentStatus.SUSPENDED) {
            equipment.changeStatus(EquipmentStatus.SUSPENDED);
            return AdminActionType.SUSPEND_EQUIPMENT;
        }

        if (requestedStatus == EquipmentStatus.INACTIVE) {
            equipment.changeStatus(EquipmentStatus.INACTIVE);
            return AdminActionType.RESTORE_EQUIPMENT;
        }

        throw new CustomException(ErrorCode.INVALID_EQUIPMENT_STATUS_TRANSITION);
    }

    // 한 페이지에 포함된 장비 등록자를 한 번에 조회해 ID 기준 Map으로 변환합니다.
    private Map<Long, User> loadOwners(List<Equipment> equipment) {
        if (equipment.isEmpty()) {
            return Map.of();
        }

        List<Long> ownerIds = equipment.stream()
                .map(Equipment::getOwnerId)
                .distinct()
                .toList();

        return userRepository.findAllById(ownerIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    // 한 페이지에 포함된 장비의 썸네일을 한 번에 조회해 장비 ID 기준 Map으로 변환합니다.
    private Map<Long, String> loadThumbnails(List<Equipment> equipment) {
        if (equipment.isEmpty()) {
            return Map.of();
        }

        Collection<Long> equipmentIds = equipment.stream().map(Equipment::getId).toList();

        return equipmentImageRepository
                .findByEquipment_IdInAndThumbnailTrueOrderBySortOrderAscIdAsc(equipmentIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getEquipment().getId(),
                        EquipmentImage::getImageUrl,
                        (first, ignored) -> first
                ));
    }

    // 등록자 Map에서 회원을 찾고 데이터가 없으면 예외를 발생시킵니다.
    private User getRequiredOwner(Map<Long, User> ownerMap, Long ownerId) {
        User owner = ownerMap.get(ownerId);

        if (owner == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return owner;
    }

    // 검색 문자열의 앞뒤 공백을 제거하고 빈 문자열은 조회 조건에서 제외합니다.
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
