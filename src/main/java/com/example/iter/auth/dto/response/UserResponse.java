package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;

public record UserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String phone,
        Role role,
        UserStatus status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
