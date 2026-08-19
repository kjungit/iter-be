package com.example.iter.auth.service.model;

public record IssuedOAuthPendingToken(
        String rawToken,
        long expiresIn
) {
}
