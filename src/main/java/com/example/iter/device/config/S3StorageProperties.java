package com.example.iter.device.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {

    @NotBlank
    private String bucket;

    @NotBlank
    private String region;

    private String publicBaseUrl;

    @NotBlank
    private String keyPrefix = "equipment";

    private Duration presignedUrlValidity = Duration.ofMinutes(5);
}
