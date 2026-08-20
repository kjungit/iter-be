package com.example.iter.device.storage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

public record PresignedUpload(
        String objectKey,
        URL uploadUrl,
        Map<String, String> requiredHeaders,
        LocalDateTime expiresAt
) {
}
