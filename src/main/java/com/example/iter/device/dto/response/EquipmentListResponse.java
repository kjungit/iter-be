package com.example.iter.device.dto.response;

import java.util.List;

public record EquipmentListResponse(
        List<EquipmentSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public EquipmentListResponse {
        content = List.copyOf(content);
    }
}
