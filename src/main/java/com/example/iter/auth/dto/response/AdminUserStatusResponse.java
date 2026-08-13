package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserStatusResponse(
        Long userId,
        UserStatus status,
        LocalDateTime updatedAt
) {
    public static AdminUserStatusResponse from(User user) {
        return new AdminUserStatusResponse(
                user.getId(),
                user.getStatus(),
                user.getUpdatedAt()
        );
    }
}
