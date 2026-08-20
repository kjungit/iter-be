package com.example.iter.auth.controller.api;

import com.example.iter.auth.controller.api.spec.OAuth2AccountApiSpec;
import com.example.iter.auth.dto.request.OAuthLinkRequest;
import com.example.iter.auth.service.OAuth2AuthService;
import com.example.iter.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/oauth2/kakao")
@RequiredArgsConstructor
public class OAuth2AccountApiController implements OAuth2AccountApiSpec {

    private final OAuth2AuthService oAuth2AuthService;

    @Override
    @PostMapping("/link")
    public ResponseEntity<Void> link(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OAuthLinkRequest request
    ) {
        oAuth2AuthService.link(principal.getUser().getId(), request.oauthToken());
        return ResponseEntity.noContent().build();
    }
}
