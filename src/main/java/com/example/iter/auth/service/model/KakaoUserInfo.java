package com.example.iter.auth.service.model;

public record KakaoUserInfo(
        String providerUserId,
        String email,
        String nickname
) {
}
