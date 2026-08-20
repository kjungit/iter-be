package com.example.iter.auth.service.model;

import com.example.iter.auth.domain.entity.OAuthProvider;

public record ConsumedOAuthToken(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String nickname,
        Long targetUserId
) {
}
