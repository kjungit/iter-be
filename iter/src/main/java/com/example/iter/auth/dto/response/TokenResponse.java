package com.example.iter.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
