package com.example.iter.device.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record PresignedImageUploadItemResponse(
        String objectKey,
        String uploadUrl,
        Map<String, String> requiredHeaders,
        LocalDateTime expiresAt
) {
}
