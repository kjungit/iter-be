package com.example.iter.reservation.service;

import com.example.iter.device.config.S3StorageProperties;
import com.example.iter.reservation.dto.request.EvidenceImagePresignRequest;
import com.example.iter.reservation.dto.response.EvidenceImagePresignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// 대여 수령/반납 증빙 사진 업로드 — device 도메인의 S3Client/S3Presigner/S3StorageProperties
// 빈을 그대로 재사용한다(같은 버킷, 별도 요청/응답 DTO). 장비 이미지와 달리 임시 업로드
// 추적 엔티티나 승격(promote) 단계가 없다 — 발급 즉시 최종 위치에 저장되는 단순한 흐름.
//
// 객체 키는 반드시 "{keyPrefix}/public/" 밑에 둬야 한다 — 백엔드 IAM 정책(infra/aws/s3/template.yaml의
// PromoteAndDeleteFinalImages)과 버킷 공개 읽기 정책(AllowPublicReadForPromotedImagesOnly)이
// 이 경로에만 PutObject/공개 GetObject를 허용하기 때문. 별도 prefix(예: rental-evidence/)를
// 쓰면 presigned URL 발급 자체는 성공하지만 실제 PUT 시점에 S3가 403으로 거부한다.
@Service
@RequiredArgsConstructor
public class RentalEvidenceUploadService {

    private static final String RENTAL_EVIDENCE_DIR = "rental-evidence/";

    private final S3Presigner s3Presigner;
    private final S3StorageProperties s3StorageProperties;

    public EvidenceImagePresignResponse createPresignedUploads(EvidenceImagePresignRequest request) {
        var uploads = request.files().stream()
                .map(this::presignOne)
                .toList();
        return new EvidenceImagePresignResponse(uploads);
    }

    private EvidenceImagePresignResponse.Item presignOne(EvidenceImagePresignRequest.Item file) {
        String extension = extensionFor(file.contentType());
        String objectKey = s3StorageProperties.getKeyPrefix() + "/public/" + RENTAL_EVIDENCE_DIR
                + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3StorageProperties.getBucket())
                .key(objectKey)
                .contentType(file.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(s3StorageProperties.getPresignedUrlValidity())
                .putObjectRequest(putObjectRequest)
                .build();

        var presigned = s3Presigner.presignPutObject(presignRequest);

        return new EvidenceImagePresignResponse.Item(
                objectKey,
                presigned.url().toString(),
                Map.of("Content-Type", file.contentType()),
                publicUrl(objectKey),
                LocalDateTime.now().plus(s3StorageProperties.getPresignedUrlValidity())
        );
    }

    private String publicUrl(String objectKey) {
        String base = s3StorageProperties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://%s.s3.%s.amazonaws.com".formatted(
                    s3StorageProperties.getBucket(), s3StorageProperties.getRegion());
        }
        return base.endsWith("/") ? base + objectKey : base + "/" + objectKey;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("지원하지 않는 Content-Type: " + contentType);
        };
    }
}
