package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminEquipmentStatusRequest(
        @NotNull(message = "변경할 장비 상태는 필수입니다.")
        EquipmentStatus status,

        @NotBlank(message = "관리자 처리 사유는 필수입니다.")
        @Size(max = 500, message = "관리자 처리 사유는 500자 이하여야 합니다.")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "관리자는 장비 상태를 INACTIVE 또는 SUSPENDED로만 변경할 수 있습니다.")
    public boolean isAllowedStatus() {
        return status == null
                || status == EquipmentStatus.INACTIVE
                || status == EquipmentStatus.SUSPENDED;
    }
}
