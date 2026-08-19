package com.example.iter.device.dto.request;

import com.example.iter.device.domain.entity.EquipmentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record EquipmentStatusUpdateRequest(
        @NotNull(message = "변경할 장비 상태는 필수입니다.")
        EquipmentStatus status
) {

    @AssertTrue(message = "장비 상태는 ACTIVE 또는 INACTIVE만 요청할 수 있습니다.")
    public boolean isOwnerManageableStatus() {
        return status == null
                || status == EquipmentStatus.ACTIVE
                || status == EquipmentStatus.INACTIVE;
    }
}
