package com.example.iter.auth.controller.api.spec;

import com.example.iter.auth.dto.request.OAuthLinkRequest;
import com.example.iter.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "OAuth2 Account", description = "내 OAuth2 계정 연결 API")
@SecurityRequirement(name = "JWT")
public interface OAuth2AccountApiSpec {

    @Operation(summary = "기존 계정에 카카오 연결")
    ResponseEntity<Void> link(
            @Parameter(hidden = true) CustomUserDetails principal,
            OAuthLinkRequest request
    );
}
