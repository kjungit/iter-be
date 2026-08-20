package com.example.iter.auth.dto.request;

import jakarta.validation.constraints.Size;

public record UserDeleteRequest(
        @Size(min = 8, max = 32, message = "비밀번호는 8자 이상 32자 이하여야 합니다.")
        String password
) {
}
