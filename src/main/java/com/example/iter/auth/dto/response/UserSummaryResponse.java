package com.example.iter.auth.dto.response;

import com.example.iter.auth.domain.entity.User;

public record UserSummaryResponse(
        Long userId,
        String nickName
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getNickname());
    }
}
