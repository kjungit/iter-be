package com.example.iter.device.support;

import com.example.iter.device.config.S3StorageProperties;
import com.example.iter.device.domain.entity.EquipmentImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

@Component
@RequiredArgsConstructor
public class EquipmentImageUrlResolver {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public String resolve(EquipmentImage image) {
        if (!StringUtils.hasText(image.getObjectKey())) {
            return image.getImageUrl();
        }
        return resolve(image.getObjectKey());
    }

    public String resolve(String objectKey) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return stripTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
        }
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build())
                .toExternalForm();
    }

    private String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
