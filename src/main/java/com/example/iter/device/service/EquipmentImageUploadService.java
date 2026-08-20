package com.example.iter.device.service;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.domain.entity.EquipmentImageUpload;
import com.example.iter.device.domain.repository.EquipmentImageUploadRepository;
import com.example.iter.device.dto.request.PresignedImageUploadRequest;
import com.example.iter.device.dto.response.PresignedImageUploadItemResponse;
import com.example.iter.device.dto.response.PresignedImageUploadResponse;
import com.example.iter.device.storage.EquipmentImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipmentImageUploadService {

    private final EquipmentImageUploadRepository uploadRepository;
    private final EquipmentImageStorage imageStorage;
    private final EquipmentImagePolicy imagePolicy;

    @Transactional
    public PresignedImageUploadResponse issue(
            User user,
            PresignedImageUploadRequest request
    ) {
        validateActiveUser(user);

        var uploads = request.files().stream()
                .map(file -> {
                    imagePolicy.validateMetadata(file.contentType(), file.size());
                    var presigned = imageStorage.createPresignedUpload(
                            user.getId(), file.contentType(), file.size());
                    uploadRepository.save(EquipmentImageUpload.builder()
                            .userId(user.getId())
                            .objectKey(presigned.objectKey())
                            .expectedContentType(file.contentType())
                            .expectedSize(file.size())
                            .expiresAt(presigned.expiresAt())
                            .build());
                    return new PresignedImageUploadItemResponse(
                            presigned.objectKey(),
                            presigned.uploadUrl().toExternalForm(),
                            presigned.requiredHeaders(),
                            presigned.expiresAt()
                    );
                })
                .toList();
        return new PresignedImageUploadResponse(uploads);
    }

    private void validateActiveUser(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
    }
}
