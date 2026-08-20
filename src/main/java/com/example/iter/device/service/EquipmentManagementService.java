package com.example.iter.device.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.auth.domain.repository.UserRepository;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.Equipment;
import com.example.iter.device.domain.entity.EquipmentImage;
import com.example.iter.device.domain.entity.EquipmentStatus;
import com.example.iter.device.domain.entity.EquipmentImageUpload;
import com.example.iter.device.domain.entity.ProductConditionType;
import com.example.iter.device.domain.repository.EquipmentImageRepository;
import com.example.iter.device.domain.repository.EquipmentImageUploadRepository;
import com.example.iter.device.domain.repository.EquipmentRepository;
import com.example.iter.device.dto.request.EquipmentCreateRequest;
import com.example.iter.device.dto.request.EquipmentImageCreateRequest;
import com.example.iter.device.dto.request.EquipmentStatusUpdateRequest;
import com.example.iter.device.dto.request.EquipmentUpdateRequest;
import com.example.iter.device.dto.response.EquipmentDetailResponse;
import com.example.iter.device.dto.response.EquipmentImageResponse;
import com.example.iter.device.dto.response.EquipmentOwnerResponse;
import com.example.iter.device.dto.response.EquipmentStatusResponse;
import com.example.iter.device.storage.EquipmentImageStorage;
import com.example.iter.device.storage.StoredImage;
import com.example.iter.device.storage.ValidatedUpload;
import com.example.iter.device.support.EquipmentImageUrlResolver;
import com.example.iter.reservation.domain.entity.RentalStatus;
import com.example.iter.reservation.domain.policy.RentalConflictPolicy;
import com.example.iter.reservation.domain.policy.RentalStatusPolicy;
import com.example.iter.reservation.domain.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentManagementService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentImageRepository equipmentImageRepository;
    private final EquipmentImageUploadRepository imageUploadRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final EquipmentImageStorage imageStorage;
    private final EquipmentImageCleanupService imageCleanupService;
    private final EquipmentImageUrlResolver imageUrlResolver;

    @Transactional
    public EquipmentDetailResponse create(
            User owner,
            EquipmentCreateRequest request
    ) {
        validateCanCreate(owner);
        List<EquipmentImageUpload> uploadRecords = findAndValidateUploadRecords(
                owner.getId(), request.imageKeys());
        List<ValidatedUpload> validatedUploads = uploadRecords.stream()
                .map(upload -> imageStorage.validateTemporaryUpload(
                        upload.getObjectKey(),
                        upload.getExpectedContentType(),
                        upload.getExpectedSize()))
                .toList();

        Equipment equipment = equipmentRepository.saveAndFlush(Equipment.builder()
                .ownerId(owner.getId())
                .category(request.category())
                .name(request.name().trim())
                .description(request.description().trim())
                .dailyPrice(request.dailyPrice())
                .availableFrom(request.availableFrom())
                .availableTo(request.availableTo())
                .status(EquipmentStatus.ACTIVE)
                .productCondition(request.productCondition())
                .conditionDetail(normalizeConditionDetail(
                        request.productCondition(), request.conditionDetail()))
                .build());

        List<StoredImage> storedImages = promoteAll(equipment.getId(), validatedUploads);
        registerStorageSynchronization(storedImages, request.imageKeys());
        LocalDateTime usedAt = LocalDateTime.now();
        uploadRecords.forEach(upload -> upload.use(usedAt));

        for (int index = 0; index < storedImages.size(); index++) {
            StoredImage storedImage = storedImages.get(index);
            equipmentImageRepository.save(EquipmentImage.builder()
                    .equipment(equipment)
                    .imageUrl(storedImage.imageUrl())
                    .objectKey(storedImage.objectKey())
                    .sortOrder(index)
                    .thumbnail(index == request.thumbnailIndex())
                    .build());
        }

        equipmentImageRepository.flush();
        return toDetailResponse(equipment.getId());
    }

    @Transactional
    public List<EquipmentImageResponse> addImages(
            User owner,
            Long equipmentId,
            EquipmentImageCreateRequest request
    ) {
        validateCanCreate(owner);
        Equipment equipment = findOwnedForUpdate(
                equipmentId, owner.getId(), "본인 소유의 장비에만 이미지를 추가할 수 있습니다.");
        List<EquipmentImage> existingImages = equipmentImageRepository
                .findByEquipmentIdOrderBySortOrderAscIdAsc(equipmentId);
        if (existingImages.size() + request.imageKeys().size() > EquipmentImagePolicy.MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        List<EquipmentImageUpload> uploadRecords = findAndValidateUploadRecords(
                owner.getId(), request.imageKeys());
        List<ValidatedUpload> validatedUploads = uploadRecords.stream()
                .map(upload -> imageStorage.validateTemporaryUpload(
                        upload.getObjectKey(),
                        upload.getExpectedContentType(),
                        upload.getExpectedSize()))
                .toList();
        List<StoredImage> storedImages = promoteAll(equipmentId, validatedUploads);
        registerStorageSynchronization(storedImages, request.imageKeys());

        if (request.thumbnailIndex() != null) {
            existingImages.forEach(image -> image.changeThumbnail(false));
        }
        int nextSortOrder = existingImages.stream()
                .mapToInt(EquipmentImage::getSortOrder)
                .max()
                .orElse(-1) + 1;
        for (int index = 0; index < storedImages.size(); index++) {
            StoredImage storedImage = storedImages.get(index);
            equipmentImageRepository.save(EquipmentImage.builder()
                    .equipment(equipment)
                    .imageUrl(storedImage.imageUrl())
                    .objectKey(storedImage.objectKey())
                    .sortOrder(nextSortOrder + index)
                    .thumbnail(request.thumbnailIndex() != null
                            && index == request.thumbnailIndex())
                    .build());
        }
        LocalDateTime usedAt = LocalDateTime.now();
        uploadRecords.forEach(upload -> upload.use(usedAt));
        equipmentImageRepository.flush();

        return equipmentImageRepository.findByEquipmentIdOrderBySortOrderAscIdAsc(equipmentId)
                .stream()
                .map(image -> EquipmentImageResponse.from(image, imageUrlResolver.resolve(image)))
                .toList();
    }

    @Transactional
    public void deleteImage(Long ownerId, Long equipmentId, Long imageId) {
        findOwnedForUpdate(
                equipmentId, ownerId, "본인 소유의 장비 이미지만 삭제할 수 있습니다.");
        List<EquipmentImage> images = equipmentImageRepository
                .findByEquipmentIdOrderBySortOrderAscIdAsc(equipmentId);
        EquipmentImage target = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_IMAGE_NOT_FOUND));
        if (images.size() <= EquipmentImagePolicy.MIN_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.MINIMUM_IMAGE_REQUIRED);
        }

        List<EquipmentImage> remainingImages = images.stream()
                .filter(image -> !image.getId().equals(imageId))
                .toList();
        if (target.isThumbnail()) {
            remainingImages.getFirst().changeThumbnail(true);
        }
        for (int index = 0; index < remainingImages.size(); index++) {
            remainingImages.get(index).changeSortOrder(index);
        }
        equipmentImageRepository.delete(target);
        equipmentImageRepository.flush();
        registerDeleteAfterCommit(target.getObjectKey());
    }

    @Transactional
    public EquipmentDetailResponse update(
            Long ownerId,
            Long equipmentId,
            EquipmentUpdateRequest request
    ) {
        Equipment equipment = findOwnedForUpdate(equipmentId, ownerId, "본인 소유의 장비만 수정할 수 있습니다.");

        String name = request.name() == null ? equipment.getName() : request.name().trim();
        String description = request.description() == null
                ? equipment.getDescription()
                : request.description().trim();
        BigDecimal dailyPrice = request.dailyPrice() == null
                ? equipment.getDailyPrice()
                : request.dailyPrice();
        LocalDate availableFrom = request.availableFrom() == null
                ? equipment.getAvailableFrom()
                : request.availableFrom();
        LocalDate availableTo = request.availableTo() == null
                ? equipment.getAvailableTo()
                : request.availableTo();
        ProductConditionType productCondition = request.productCondition() == null
                ? equipment.getProductCondition()
                : request.productCondition();
        String conditionDetail = request.conditionDetail() == null
                ? equipment.getConditionDetail()
                : request.conditionDetail().trim();

        validateUpdateValues(availableFrom, availableTo, productCondition, conditionDetail);
        validateExistingRentalsRemainIncluded(equipment, availableFrom, availableTo);

        equipment.update(
                name,
                description,
                dailyPrice,
                availableFrom,
                availableTo,
                productCondition,
                normalizeConditionDetail(productCondition, conditionDetail)
        );
        equipmentRepository.flush();
        return toDetailResponse(equipmentId);
    }

    @Transactional
    public void delete(Long ownerId, Long equipmentId) {
        Equipment equipment = findOwnedForUpdate(equipmentId, ownerId, "본인 소유의 장비만 삭제할 수 있습니다.");

        if (rentalRepository.existsByEquipmentIdAndStatus(equipmentId, RentalStatus.DISPUTED)) {
            throw new CustomException(ErrorCode.ACTIVE_DISPUTE_EXISTS);
        }
        if (rentalRepository.existsByEquipmentIdAndStatusIn(
                equipmentId, RentalStatusPolicy.equipmentDeletionBlockingStatuses())) {
            throw new CustomException(ErrorCode.ACTIVE_RENTAL_EXISTS);
        }

        equipment.delete();
        equipmentRepository.flush();
    }

    @Transactional
    public EquipmentStatusResponse updateStatus(
            User owner,
            Long equipmentId,
            EquipmentStatusUpdateRequest request
    ) {
        Equipment equipment = findOwnedForUpdate(
                equipmentId, owner.getId(), "해당 장비의 상태를 변경할 권한이 없습니다.");

        if (equipment.getStatus() == EquipmentStatus.SUSPENDED
                || equipment.getStatus() == EquipmentStatus.DELETED
                || equipment.getStatus() == request.status()) {
            throw new CustomException(ErrorCode.EQUIPMENT_STATUS_CHANGE_NOT_ALLOWED);
        }
        if (request.status() == EquipmentStatus.ACTIVE) {
            validateCanCreate(owner);
        }

        equipment.changeStatus(request.status());
        equipmentRepository.flush();
        return new EquipmentStatusResponse(
                equipment.getId(),
                equipment.getStatus(),
                equipment.getUpdatedAt()
        );
    }

    private Equipment findOwnedForUpdate(Long equipmentId, Long ownerId, String forbiddenMessage) {
        Equipment equipment = equipmentRepository.findByIdForUpdate(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.EQUIPMENT_NOT_FOUND, "존재하지 않는 장비입니다."));
        if (equipment.getStatus() == EquipmentStatus.DELETED) {
            throw new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND, "존재하지 않는 장비입니다.");
        }
        if (!equipment.isOwnedBy(ownerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, forbiddenMessage);
        }
        return equipment;
    }

    private void validateCanCreate(User owner) {
        if (owner.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        if (owner.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
    }

    private void validateUpdateValues(
            LocalDate availableFrom,
            LocalDate availableTo,
            ProductConditionType productCondition,
            String conditionDetail
    ) {
        if (availableFrom == null
                || availableTo == null
                || availableTo.isBefore(availableFrom)
                || productCondition != ProductConditionType.NORMAL
                && (conditionDetail == null || conditionDetail.isBlank())) {
            throw new CustomException(
                    ErrorCode.VALIDATION_ERROR,
                    "장비 수정 정보를 올바르게 입력해주세요."
            );
        }
    }

    private void validateExistingRentalsRemainIncluded(
            Equipment equipment,
            LocalDate availableFrom,
            LocalDate availableTo
    ) {
        boolean periodChanged = !availableFrom.equals(equipment.getAvailableFrom())
                || !availableTo.equals(equipment.getAvailableTo());
        if (periodChanged && rentalRepository.existsOccupyingRentalOutsidePeriod(
                equipment.getId(),
                availableFrom,
                availableTo,
                RentalConflictPolicy.nonOccupyingStatuses())) {
            throw new CustomException(
                    ErrorCode.ACTIVE_RENTAL_EXISTS,
                    "기존 예약을 제외하는 기간으로 대여 가능 기간을 변경할 수 없습니다."
            );
        }
    }

    private String normalizeConditionDetail(
            ProductConditionType productCondition,
            String conditionDetail
    ) {
        if (productCondition == ProductConditionType.NORMAL) {
            return null;
        }
        return conditionDetail == null ? null : conditionDetail.trim();
    }

    private EquipmentDetailResponse toDetailResponse(Long equipmentId) {
        var row = equipmentRepository.findManagementDetailById(equipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
        Equipment equipment = row.equipment();
        var owner = userRepository.findSummaryById(equipment.getOwnerId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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

    private List<EquipmentImageUpload> findAndValidateUploadRecords(
            Long ownerId,
            List<String> objectKeys
    ) {
        List<EquipmentImageUpload> records = imageUploadRepository
                .findAllByObjectKeyInForUpdate(objectKeys);
        Map<String, EquipmentImageUpload> recordsByKey = new LinkedHashMap<>();
        records.forEach(record -> recordsByKey.put(record.getObjectKey(), record));

        LocalDateTime now = LocalDateTime.now();
        return objectKeys.stream()
                .map(objectKey -> {
                    EquipmentImageUpload record = recordsByKey.get(objectKey);
                    if (record == null || !record.getUserId().equals(ownerId)) {
                        throw new CustomException(ErrorCode.IMAGE_UPLOAD_NOT_FOUND);
                    }
                    if (record.isUsed()) {
                        throw new CustomException(ErrorCode.IMAGE_UPLOAD_ALREADY_USED);
                    }
                    if (record.isExpired(now)) {
                        throw new CustomException(ErrorCode.IMAGE_UPLOAD_EXPIRED);
                    }
                    return record;
                })
                .toList();
    }

    private List<StoredImage> promoteAll(
            Long equipmentId,
            List<ValidatedUpload> validatedUploads
    ) {
        List<StoredImage> promoted = new ArrayList<>();
        try {
            for (ValidatedUpload validatedUpload : validatedUploads) {
                promoted.add(imageStorage.promote(equipmentId, validatedUpload));
            }
            return List.copyOf(promoted);
        } catch (RuntimeException exception) {
            promoted.forEach(image -> deleteQuietly(
                    image.objectKey(), "부분 승격된 장비 이미지 삭제 실패"));
            throw exception;
        }
    }

    private void registerStorageSynchronization(
            List<StoredImage> storedImages,
            List<String> temporaryObjectKeys
    ) {
        List<String> temporaryKeys = List.copyOf(temporaryObjectKeys);
        List<String> storedKeys = storedImages.stream()
                .map(StoredImage::objectKey)
                .toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageCleanupService.deleteAll(
                        temporaryKeys, "사용 완료된 임시 장비 이미지 삭제 실패");
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                imageCleanupService.deleteAll(
                        storedKeys, "롤백된 최종 장비 이미지 삭제 실패");
            }
        });
    }

    private void registerDeleteAfterCommit(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageCleanupService.deleteAll(
                        List.of(objectKey), "삭제된 장비 이미지 객체 정리 실패");
            }
        });
    }

    // 승격 도중 실패한 객체는 요청 실패 전에 즉시 보상 삭제해야 하므로 동기로 처리합니다.
    private void deleteQuietly(String objectKey, String failureMessage) {
        try {
            imageStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.error("{}: objectKey={}", failureMessage, objectKey, exception);
        }
    }
}
