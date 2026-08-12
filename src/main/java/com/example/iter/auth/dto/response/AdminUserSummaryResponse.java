package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        Long userId,
        String email,
        String name,
        String nickName,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
