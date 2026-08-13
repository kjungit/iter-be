package com.example.iter.device.dto.response;

public record ImageResponse(
        Long imageId,
        String imageUrl,
        int sortOrder,
        boolean thumbnail
) {
}
