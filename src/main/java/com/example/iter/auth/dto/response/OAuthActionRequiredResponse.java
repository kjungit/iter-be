package com.example.iter.auth.dto.response;

public record OAuthActionRequiredResponse(
        OAuthAction action,
        String oauthToken,
        String email,
        String nickname,
        long expiresIn
) {
}
