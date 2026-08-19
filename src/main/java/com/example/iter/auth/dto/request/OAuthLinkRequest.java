package com.example.iter.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthLinkRequest(
        @NotBlank(message = "OAuth 연결 토큰은 필수입니다.")
        String oauthToken
) {
}
