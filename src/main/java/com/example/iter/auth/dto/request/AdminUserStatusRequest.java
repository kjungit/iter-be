package com.example.iter.auth.dto.request;

import com.example.iter.auth.domain.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserStatusRequest(
        @NotNull(message = "변경할 회원 상태는 필수입니다.")
        UserStatus status,

        @NotBlank(message = "관리자 처리 사유는 필수입니다.")
        @Size(max = 500, message = "관리자 처리 사유는 500자 이하여야 합니다.")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "회원 상태는 ACTIVE 또는 SUSPENDED로만 변경할 수 있습니다.")
    public boolean isAllowedStatus() {
        return status == null
                || status == UserStatus.ACTIVE
                || status == UserStatus.SUSPENDED;
    }
}
