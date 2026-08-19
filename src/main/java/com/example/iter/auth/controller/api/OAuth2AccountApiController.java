package com.example.iter.auth.controller.api;

import com.example.iter.auth.dto.request.OAuthLinkRequest;
import com.example.iter.auth.service.OAuth2AuthService;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth2 Account", description = "내 OAuth2 계정 연결 API")
@RestController
@RequestMapping("/api/v1/users/me/oauth2/kakao")
@RequiredArgsConstructor
public class OAuth2AccountApiController {

    private final OAuth2AuthService oAuth2AuthService;

    @Operation(
            summary = "기존 계정에 카카오 연결",
            security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping("/link")
    public ResponseEntity<Void> link(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OAuthLinkRequest request
    ) {
        oAuth2AuthService.link(principal.getUser().getId(), request.oauthToken());
        return ResponseEntity.noContent().build();
    }
}
