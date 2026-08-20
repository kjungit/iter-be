package com.example.iter.device.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresignedImageFileRequest(
        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @NotBlank(message = "이미지 Content-Type은 필수입니다.")
        String contentType,

        @Positive(message = "이미지 크기는 0보다 커야 합니다.")
        long size
) {
}
