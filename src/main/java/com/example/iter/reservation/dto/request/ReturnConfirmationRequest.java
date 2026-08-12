package com.example.iter.reservation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public record ReturnConfirmationRequest(
        @NotNull(message = "상품 이상 여부는 필수입니다.")
        Boolean hasIssue,

        @Size(max = 50, message = "분쟁 사유는 50자 이하여야 합니다.")
        String disputeReason,

        @Size(max = 2000, message = "분쟁 설명은 2000자 이하여야 합니다.")
        String disputeDescription
) {
    @JsonIgnore
    @AssertTrue(
            message = "상품에 이상이 있으면 분쟁 사유와 설명이 필요합니다."
    )
    public boolean isDisputeInputValid() {
        if (hasIssue == null) {
            return true;
        }

        boolean hasReason = disputeReason != null && !disputeReason.isBlank();
        boolean hasDescription = disputeDescription != null && !disputeDescription.isBlank();

        if (hasIssue) {
            return hasReason && hasDescription;
        }

        return !hasReason && !hasDescription;
    }
}
