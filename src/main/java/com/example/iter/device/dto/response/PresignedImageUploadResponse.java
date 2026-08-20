package com.example.iter.device.dto.response;

import java.util.List;

public record PresignedImageUploadResponse(
        List<PresignedImageUploadItemResponse> uploads
) {
}
