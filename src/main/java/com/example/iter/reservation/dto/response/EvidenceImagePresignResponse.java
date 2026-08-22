package com.example.iter.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EvidenceImagePresignResponse(
        List<Item> uploads
) {
    public record Item(
            String objectKey,
            String uploadUrl,
            Map<String, String> requiredHeaders,
            String publicUrl,
            LocalDateTime expiresAt
    ) {
    }
}
