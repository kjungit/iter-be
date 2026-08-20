package com.example.iter.device.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignedImageUploadRequest(
        @NotNull(message = "업로드할 이미지 목록은 필수입니다.")
        @Size(min = 1, max = 5, message = "이미지는 1장 이상 5장 이하로 요청해주세요.")
        List<@Valid PresignedImageFileRequest> files
) {
}
