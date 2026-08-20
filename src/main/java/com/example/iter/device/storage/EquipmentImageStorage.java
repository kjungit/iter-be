package com.example.iter.device.storage;

public interface EquipmentImageStorage {

    PresignedUpload createPresignedUpload(
            Long userId,
            String contentType,
            long expectedSize
    );

    ValidatedUpload validateTemporaryUpload(
            String objectKey,
            String expectedContentType,
            long expectedSize
    );

    StoredImage promote(Long equipmentId, ValidatedUpload upload);

    void delete(String objectKey);
}
