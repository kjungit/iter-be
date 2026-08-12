package com.example.iter.reservation.dto.response;

import com.example.iter.reservation.domain.entity.ProductConditionType;

import java.time.LocalDateTime;
import java.util.List;

public record ConditionEvidenceResponse(
        ProductConditionType productCondition,
        String conditionDetail,
        List<String> imageUrls,
        LocalDateTime recordedAt
) {
    public ConditionEvidenceResponse {
        imageUrls = imageUrls == null
                ? List.of()
                : List.copyOf(imageUrls);
    }
}
