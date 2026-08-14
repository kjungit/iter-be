package com.example.iter.auth.service.model;

public record IssuedTokenPair(
        String accessToken,
        String refreshToken
) {
}
