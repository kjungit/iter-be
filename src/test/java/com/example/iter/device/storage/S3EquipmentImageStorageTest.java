package com.example.iter.device.storage;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;
import com.example.iter.device.config.S3StorageProperties;
import com.example.iter.device.service.EquipmentImagePolicy;
import com.example.iter.device.support.EquipmentImageUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3EquipmentImageStorageTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3EquipmentImageStorage storage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        S3StorageProperties properties = new S3StorageProperties();
        properties.setBucket("iter-test");
        properties.setRegion("ap-northeast-2");
        properties.setKeyPrefix("/equipment/");
        properties.setPublicBaseUrl("https://cdn.example.com/");
        properties.setPresignedUrlValidity(Duration.ofMinutes(5));
        EquipmentImagePolicy policy = new EquipmentImagePolicy();
        EquipmentImageUrlResolver resolver = new EquipmentImageUrlResolver(
                s3Client, properties);
        storage = new S3EquipmentImageStorage(
                s3Client, s3Presigner, properties, policy, resolver);
    }

    @Test
    void 사용자별_임시_경로의_Presigned_PUT_URL을_발급한다() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(
                URI.create("https://s3.example.com/upload?signature=value").toURL());
        when(presigned.signedHeaders()).thenReturn(Map.of(
                "host", List.of("s3.example.com"),
                "content-type", List.of("image/jpeg"),
                "content-length", List.of("1024")));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presigned);

        PresignedUpload upload = storage.createPresignedUpload(42L, "image/jpeg", 1024);

        assertThat(upload.objectKey())
                .startsWith("equipment/temp/42/")
                .endsWith(".jpg");
        assertThat(upload.requiredHeaders())
                .containsEntry("content-type", "image/jpeg")
                .doesNotContainKeys("host", "content-length");
        ArgumentCaptor<PutObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(requestCaptor.capture());
        PutObjectRequest putObjectRequest = requestCaptor.getValue().putObjectRequest();
        assertThat(putObjectRequest.contentLength()).isEqualTo(1024L);
    }

    @Test
    void 임시_객체의_실제_메타데이터와_매직넘버를_검증한다() {
        String objectKey = "equipment/temp/42/image.jpg";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(1024L)
                        .eTag("etag-1")
                        .build());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().eTag("etag-1").build(),
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));

        ValidatedUpload upload = storage.validateTemporaryUpload(
                objectKey, "image/jpeg", 1024L);

        assertThat(upload.objectKey()).isEqualTo(objectKey);
        assertThat(upload.eTag()).isEqualTo("etag-1");
        ArgumentCaptor<GetObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertThat(requestCaptor.getValue().range()).isEqualTo("bytes=0-11");
    }

    @Test
    void 검증한_ETag를_조건으로_최종_경로에_승격하고_캐시_헤더를_설정한다() {
        ValidatedUpload upload = new ValidatedUpload(
                "equipment/temp/42/image.jpg", "image/jpeg", 1024L, "etag-1");

        StoredImage storedImage = storage.promote(7L, upload);

        ArgumentCaptor<CopyObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(requestCaptor.capture());
        CopyObjectRequest request = requestCaptor.getValue();
        assertThat(request.copySourceIfMatch()).isEqualTo("etag-1");
        assertThat(request.destinationKey())
                .startsWith("equipment/public/7/")
                .endsWith(".jpg");
        assertThat(request.cacheControl())
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(storedImage.imageUrl())
                .isEqualTo("https://cdn.example.com/" + request.destinationKey());
    }

    @Test
    void 메타데이터나_실제_파일이_다르면_임시_객체를_거부한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(999L)
                        .eTag("etag-1")
                        .build());

        assertThatThrownBy(() -> storage.validateTemporaryUpload(
                "equipment/temp/42/image.jpg", "image/jpeg", 1024L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE);
    }

    @Test
    void 저장된_객체_키로_S3_이미지를_삭제한다() {
        storage.delete("equipment/public/42/image.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("iter-test");
        assertThat(requestCaptor.getValue().key()).isEqualTo("equipment/public/42/image.jpg");
    }
}
