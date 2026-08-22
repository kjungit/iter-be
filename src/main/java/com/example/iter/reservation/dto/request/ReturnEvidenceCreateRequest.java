package com.example.iter.reservation.dto.request;

import com.example.iter.reservation.domain.entity.ProductConditionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReturnEvidenceCreateRequest(
        @NotNull(message = "반납 시점 상품 상태는 필수입니다.")
        ProductConditionType productCondition,

        String conditionDetail,

        @NotEmpty(message = "반납 증빙 사진은 최소 1장 필요합니다.")
        List<@NotEmpty String> imageUrls
) {
}
