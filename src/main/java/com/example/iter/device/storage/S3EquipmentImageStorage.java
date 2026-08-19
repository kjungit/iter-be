package com.example.iter.device.storage;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.config.S3StorageProperties;
import com.example.iter.device.service.EquipmentImagePolicy;
import com.example.iter.device.support.EquipmentImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3EquipmentImageStorage implements EquipmentImageStorage {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;
    private final EquipmentImagePolicy imagePolicy;
    private final EquipmentImageUrlResolver imageUrlResolver;

    @Override
    public PresignedUpload createPresignedUpload(
            Long userId,
            String contentType,
            long expectedSize
    ) {
        imagePolicy.validateMetadata(contentType, expectedSize);
        String objectKey = "%s/temp/%d/%s.%s".formatted(
                normalizedPrefix(),
                userId,
                UUID.randomUUID(),
                imagePolicy.extensionOf(contentType)
        );

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(expectedSize)
                    .build();
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(properties.getPresignedUrlValidity())
                            .putObjectRequest(putObjectRequest)
                            .build()
            );
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plus(properties.getPresignedUrlValidity());
            return new PresignedUpload(
                    objectKey,
                    presignedRequest.url(),
                    clientRequiredHeaders(presignedRequest),
                    expiresAt
            );
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public ValidatedUpload validateTemporaryUpload(
            String objectKey,
            String expectedContentType,
            long expectedSize
    ) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            if (head.contentLength() != expectedSize
                    || !expectedContentType.equals(head.contentType())) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }
            imagePolicy.validateMetadata(head.contentType(), head.contentLength());

            ResponseBytes<GetObjectResponse> header = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey)
                            .range("bytes=0-11")
                            .build()
            );
            if (!head.eTag().equals(header.response().eTag())) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }
            imagePolicy.validateSignature(head.contentType(), header.asByteArray());
            return new ValidatedUpload(
                    objectKey,
                    head.contentType(),
                    head.contentLength(),
                    head.eTag()
            );
        } catch (CustomException exception) {
            throw exception;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public StoredImage promote(Long equipmentId, ValidatedUpload upload) {
        String finalObjectKey = "%s/public/%d/%s.%s".formatted(
                normalizedPrefix(),
                equipmentId,
                UUID.randomUUID(),
                imagePolicy.extensionOf(upload.contentType())
        );
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(properties.getBucket())
                    .sourceKey(upload.objectKey())
                    .destinationBucket(properties.getBucket())
                    .destinationKey(finalObjectKey)
                    .copySourceIfMatch(upload.eTag())
                    .metadataDirective(MetadataDirective.REPLACE)
                    .contentType(upload.contentType())
                    .cacheControl(CACHE_CONTROL)
                    .build());
            return new StoredImage(
                    finalObjectKey,
                    imageUrlResolver.resolve(finalObjectKey)
            );
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build());
    }

    private Map<String, String> clientRequiredHeaders(
            PresignedPutObjectRequest presignedRequest
    ) {
        Map<String, String> headers = new LinkedHashMap<>();
        presignedRequest.signedHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("host")
                    && !name.equalsIgnoreCase("content-length")
                    && !values.isEmpty()) {
                headers.put(name, values.getFirst());
            }
        });
        return Map.copyOf(headers);
    }

    private String normalizedPrefix() {
        String prefix = properties.getKeyPrefix().trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
