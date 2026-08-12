package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long userId,
        String email,
        String name,
        String nickName,
        String phone,
        Role role,
        UserStatus status,
        long rentedCount,
        long lentCount,
        long overdueCount,
        long reportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
